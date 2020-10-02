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

def decorate_response(status_code, msg, data):
    return jsonify({
        'status': status_code,
        'msg': msg,
        'data': data
    })

app.run(debug=True)