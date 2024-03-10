import os.path
from configparser import ConfigParser

from flask import Flask, jsonify, request

app = Flask(__name__)

# Setup the config and make sure the file is present
CONFIG_FILE = 'config.ini'
if not os.path.isfile(CONFIG_FILE):
    print(f'{CONFIG_FILE} was not found')
    # Create new config.ini file
    with open(CONFIG_FILE, 'w+') as f:
        data = "[app]\nhost        = 0.0.0.0\nport        = 5000\nendpoint    = /\ndebug       = false"
        f.write(data)

config = ConfigParser()
config.read(CONFIG_FILE)


# Store and pull notifications from this variables
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

        data = {
            'title':        request.json['title'],
            'body':         request.json['body']
        }

        print(f'Created notification\n{data}')

        notifications.append(data)
        return response(201, 'Successfully created notification', request.json)

    elif request.method == 'GET':

        if len(notifications) == 0:
            return response(200, 'There are currently no notifications')

        # Pull the notification from the stored
        latest_notification = notifications[-1]
        notifications.remove(latest_notification)

        print(f'Pulled notification\n{latest_notification}')

        # Return the latest notification
        return response(200, 'Polled last notification', latest_notification)


def response(status, message, data=None):
    response_data = {
        'status': status,
        'message': message,
    }
    # Add the data to the response if there is any
    if data:
        response_data['data'] = data

    return jsonify(response_data), status


if __name__ == "__main__":
    app.run(debug=config.getboolean('app', 'debug'),
            host=config['app']['host'], port=config.getint('app', 'port'))
