from flask import Flask, request, jsonify
app = Flask(__name__)

notifications = []

@app.route('/', methods=['GET', 'POST'])
def index():
    if request.method == 'POST':
        if not request.json == None:
            if not 'title' in request.json:
                return decorate_response(400, "Not a valid title", "")
            if not 'description' in request.json:
                return decorate_response(400, 'Not a valid description', "")

            data = {
                'title':        request.json['title'],
                'description':  request.json['description']
            }


            notifications.append(data)
            return decorate_response(200, 'Successfully posted notification', request.json)
    return decorate_response(200, 'Returned all available notifications', notifications)

@app.route('/get-latest', methods=['GET'])
def get_latest():
    if request.method == 'GET':
        try:
            _notification = notifications[-1]
            notifications.remove(notifications[-1])
            return decorate_response(200, 'The latest notification', _notification)
        except:
            return decorate_response(400, "There is no latest notifications", '')
    return ''

def decorate_response(status_code, msg, data):
    return jsonify({
        'status': status_code,
        'message': msg,
        'data': data
    })

app.run(debug=True, host="0.0.0.0")