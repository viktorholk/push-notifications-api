from flask import Flask, request, jsonify
app = Flask(__name__)

notifications = []
port = 5000
@app.route('/', methods=['GET', 'POST'])
def index():
    if request.method == 'POST':
        if not request.json == None:
            if not 'title' in request.json:
                return decorate_response(400, "Not a valid title", "")

            if not 'text' in request.json:
                return decorate_response(400, 'Not a valid text', "")

            data = {
                'title':        request.json['title'],
                'text':         request.json['text']
            }

            notifications.append(data)
            return decorate_response(200, 'Successfully posted notification', request.json)
        else:
            return decorate_response(400, 'Bad request', '')


    elif request.method == 'GET':
        try:
            _notification = notifications[-1]
            notifications.remove(notifications[-1])
            return decorate_response(200, 'The latest notification', _notification)
        except:
            return decorate_response(400, "There is new notification", '')
    return ''


def decorate_response(status_code, msg, data):
    return jsonify({
        'status': status_code,
        'message': msg,
        'data': data
    })

app.run(debug=True, host="0.0.0.0", port=port)