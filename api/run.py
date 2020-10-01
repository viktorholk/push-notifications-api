import platform
from os import system
import argparse


if __name__ == "__main__":
    # Variables
    sys             = platform.system()
    put             = ''
    configuations   = []

    # Check what os is running and se the put variable dependedet on the system
    if sys == 'Windows':
        put = 'set'
    else:
        put = 'export'

    # Set Flask app
    system(f'{put} FLASK_APP=app.py')

    # Argparse
    parser = argparse.ArgumentParser(description='Run Flask app')
    parser.add_argument('-ip', '--ipv4', help='Set the ipv4 address')
    parser.add_argument('-p', '--port', help='Set the port')
    args = parser.parse_args()
    

    # Change ip
    if args.ipv4:
        configuations.append(f'--host={args.ipv4}')
        print('Changed default ipv4')
    
    if args.port:
        configuations.append(f'--port={args.port}')
        print('Changed default port')

    # Enable development
    system(f'flask run {" ".join(configuations)}')