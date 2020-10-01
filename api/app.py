from flask import Flask
app = Flask(__name__)

@app.route('/')
def index():
    return 'API Push Notifications<br >Read the readme for more infomation.'