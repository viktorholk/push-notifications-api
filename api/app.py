from flask import Flask, request, jsonify
app = Flask(__name__)

notifications = []

@app.route('/', methods=['GET', 'POST'])
def index():
    if request.method == 'POST':
        if not request.json == None:
            notifications.append(request.json)
            return jsonify({
                'status': 201,
                'data': request.json
            })
    return jsonify({
        'status': 200,
        'notifications': notifications
    })



app.run(debug=True)