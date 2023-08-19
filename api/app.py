from configparser import ConfigParser

from flask import Flask, jsonify, request

app = Flask(__name__)

CONFIG_FILE = 'config.ini'

# Read the configuration
config = ConfigParser()
config.read(CONFIG_FILE)


if __name__ == "__main__":

    # Don't start the application if the config cannot load
    if (len(config.sections()) < 1):
        print(f'{CONFIG_FILE} was not found')
        # Create new config.ini file
        with open(CONFIG_FILE, 'w+') as f:
            data = """[app]\nhost        = 0.0.0.0\nport        = 5000\nendpoint    = /notifications/\ndebug       = false"""

            f.write(data)
            print(
                'Created new configuration.\nRestart the app to use the new configuration')
        raise SystemExit()
# Start the application
    else:
        # THe list where notifications will be stored and polled
        notifications = []

        @app.route(config['app']['endpoint'], methods=['GET', 'POST'])
        def index():
            if request.method == 'POST':

                # Only accept JSON calls
                if request.json is None:
                    return response(400, 'Bad request')

                # Check if the post request is valid
                if 'title' not in request.json:
                    return response(400, "Title was not found")

                if 'body' not in request.json:
                    return response(400, 'Body was not found')

                # Create the data from the request
                data = {
                    'title':        request.json['title'],
                    'body':         request.json['body']
                }
                print(f'Created notification\n{data}')
                notifications.append(data)
                return response(201, 'Successfully created notification', request.json)

            elif request.method == 'GET':
                if len(notifications) > 0:
                    latest_notification = notifications[-1]
                    # Pull the notification from the stored
                    notifications.remove(latest_notification)
                    print(f'Pulled notification\n{latest_notification}')
                    # Return the latest notification
                    return response(200, 'Polled last notification', latest_notification)
                else:
                    return response(200, 'There is currently no notifications')

        @app.errorhandler(400)
        def bad_request(error):
            return response(400, "Bad request")

        @app.errorhandler(404)
        def page_not_found(error):
            return response(404, "The requested URL was not found on the server")

        def response(status, message, data=None):
            # Create the response object
            response_data = {
                'status': status,
                'message': message,
            }
            # Add the data to the response if there is any
            if data:
                response_data['data'] = data

            # return the data with the statuscode
            return jsonify(response_data), status

        print(config.getint('app', 'port'))

        # Run the application
        app.run(debug=config.getboolean('app', 'debug'),
                host=config['app']['host'], port=config.getint('app', 'port'))
