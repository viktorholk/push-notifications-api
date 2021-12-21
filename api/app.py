from flask import Flask, request, jsonify
from configparser import ConfigParser
app = Flask(__name__)

CONFIG_FILE = 'config.ini'

# Read the configuration
config = ConfigParser()
config.read(CONFIG_FILE)

# Don't start the application if the config cannot load
if (len(config.sections()) < 1):
        print(f'{CONFIG_FILE} was not found')
        # Create new config.ini file
        with open(CONFIG_FILE, 'w+') as f:
            data = """[app]\nhost        = 0.0.0.0\nport        = 5000\nendpoint    = /notifications/\ndebug       = false"""

            f.write(data)
            print('Created new configuration.\nRestart the app to use the new configuration')
        raise SystemExit()
# Start the application
else:
    # THe list where notifications will be stored and polled
    notifications = []

    @app.route(config['app']['endpoint'], methods=['GET', 'POST'])
    def index():
        if request.method == 'POST':
            # Only accept JSON calls
            if not request.json == None:
                # Check if the post request is valid
                if not 'title' in request.json:
                    return response(400, "Title was not found", {})

                if not 'body' in request.json:
                    return response(400, 'Body was not found', {})

                # Create the data from the request
                data = {
                    'title':        request.json['title'],
                    'body':         request.json['body']
                }
                print(f'Created notification\n{data}')
                notifications.append(data)
                return response(200, 'Successfully created notification', request.json)
            else:
                return response(400, 'Bad request', {})

        elif request.method == 'GET':
            if len(notifications) > 0:
                latest_notification = notifications[-1]
                # Pull the notification from the stored
                notifications.remove(latest_notification)
                print(f'Pulled notification\n{latest_notification}')
                # Return the latest notification
                return response(200, 'Polled last notification', latest_notification)
            else:
                return response(404, 'There is currently no notifications', {})

        return response(400, "Bad request", {})    

    def response(status, message, data):
        return jsonify({
            'status': status,
            'message': message,
            'data': data
        })

    # Run the application
    app.run(debug=config.getboolean('app', 'debug'), host=config['app']['host'], port=config.getint('app', 'port'))
