from flask import Flask, jsonify, request, send_file
from flask_restful import Resource, Api
import requests
import math
import datetime
import os

import MySQLdb
import json

# To use error log
import sys

# ML imports
from tensorflow.keras.applications.vgg16 import VGG16
import machine_learning

app = Flask(__name__)
api = Api(app)


######### MYSQL #########

username = 'maccproject2024'
password = 'maccprojectdatabase'
hostname = 'maccproject2024.mysql.pythonanywhere-services.com'
db = 'maccproject2024$project'

######## Storage #########

photoStoragePath = "/home/maccproject2024/mysite/project-pictures/"

####### Parameters ########

# Lower values are more important
PHYSICAL_DISTANCE_IMPORTANCE = 2
TEMPORAL_DISTANCE_IMPORTANCE = 4
PHOTO_SIMILARITY_IMPORTANCE = 1

# Maximum distance (in kilometers)
MAX_DIST_KM = 100

# Maximum temporal distance (2 years before)
MAX_DIST_DAYS = 365*2

# Minimum similarity value for accepting photo
SIMILARITY_THRESHOLD = 0.12

##########################


# To convert latitude and longitude to the closest address (passed as strings)
def convertCoordinatesToAddress(latitude, longitude):

    '''
        WE USE THIS API:
        https://docs.locationiq.com/docs/quickstart-convert-coordinates-to-addresses
        Registration with email maccproject2024@gmail.com
        Our access token is: pk.6e257ab2a3c4cf56f5c0c62d604a69f2
    '''

    access_token = "pk.6e257ab2a3c4cf56f5c0c62d604a69f2"
    URL = "https://us1.locationiq.com/v1/reverse"

    # Defining a params dict for the parameters to be sent to the API
    PARAMS = {
        'key':access_token,
        'lat':latitude,
        'lon':longitude,
        'format':"json"
        }

    # Sending get request and saving the response as response object
    r = requests.get(url = URL, params = PARAMS)

    # Extracting data in json format
    data = r.json()

    # DEBUG
    #print(str(data.get("address")), file=sys.stderr)

    # Compose the address
    addr = data.get("address")

    road = addr.get("road")
    if (road is None): road = ""

    house_number = addr.get("house_number")
    if (house_number is None): house_number = ""

    city = addr.get("city")
    if (city is None): city = ""

    state = addr.get("state")
    if (state is None): state = ""

    country = addr.get("country")
    if (country is None): country = ""

    address = road + " " + house_number + ", " + city + ", " + state + ", " + country

    return address


# To compute distance in kilometers (or meters) between two coordinates
def computeGeographicDistance(lat1, lon1, lat2, lon2):
    # Earth radius in meters (to return the distance in meters)
    #R = 6371e3
    # Earth radius in kilometers (to return the distance in kilometers)
    R = 6378.14

    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)

    deltaphi = math.radians(lat2-lat1)
    deltalambda = math.radians(lon2-lon1)

    a = math.sin(deltaphi/2) * math.sin(deltaphi/2) + math.cos(phi1) * math.cos(phi2) * math.sin(deltalambda/2) * math.sin(deltalambda/2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))

    distance = R * c
    return distance


# To compute the days between two dates
def computeDateDifference(date1, date2):
    diff = date1 - date2
    return diff.days


# To obtain the latitude and the longitude from a string formatted like "40.23244, 12.28491"
def getLatitudeAndLongitudeFromString(coordinates):
    coords = coordinates.split(",")
    lat = float(coords[0].strip())
    lon = float(coords[1].strip())
    return [lat, lon]


class myHUB(Resource):
    def get(self):
        return "hello"

    def post(self):
        r = request.args['string']
        return str(r)



    ##### Database REST API ###############################################################################################

    '''
        ----POSTS-----------------------------------------------------------------------------------------------------------

        /posts

        GET ->  returns a JsonArray with 20 most recent posts, so the first 20 rows of posts table ordered by decreasing date

        POST -> create a new row in the posts table with a new post_id and it saves the corresponding image (sent in the same request) in the storage as "<post_id>.jpg"


        /photo?post_id=...

            GET ->  returns the "<post_id>.jpg" photo

        @Multipart
        /match?user_id=...&date=...&position=...

            POST -> given the user_id, the current date, the position and the picture taken to the pet, it returns the list of best matches


        /users/<user_id>/posts

            GET ->  returns a JsonArray with all posts of <user_id> user


        /post/<post_id>

            DELETE ->  it deletes the <post_id> post

        ----MESSAGES--------------------------------------------------------------------------------------------------------

        /messages?userId=...&chatNameId=...&chatId=...

            GET ->  returns messages exchanged between the pair of users identified by userId and chatNameId, selected from chat_messages table. Checks if the requesting userId user is reading the chatId chat and in that case update the relative row of chats table

        /messages

            POST -> creates a new row in the chat_message table, with a new message_id

        ----CHATS-----------------------------------------------------------------------------------------------------------

        /chat?chatId=...

            GET ->  returns the row of chats table related to chatId

        /chats?userId=...

            GET  -> returns all rows from chats table related to userId (as last sender or last receiver)

        /chats

            POST -> inserts a new row in chats table

            PUT  -> updates an existing row in chats table


        /notify?userId=...

            GET ->  checks if there is at least one row in chats with userId as last_receiver and with unread equal to 1, so with unread messages
    '''

    ##### Posts API ###############################################################################################

    @app.route("/posts", methods=['GET', 'POST'])
    def posts():

        # To see all posts
        if request.method == 'GET':

            # Connect to the database
            conn = MySQLdb.connect(host=hostname, user=username, passwd=password, db=db)
            cursor = conn.cursor()

            # Take the 10 most recent posts
            cursor.execute("SELECT * from posts ORDER BY date DESC LIMIT 20;")
            conn.commit()
            result = cursor.fetchall()
            conn.close()

            # Return as JSON
            return jsonify(result)

        # To create a new post
        elif request.method == 'POST':

            # Take request data
            data=json.loads(request.form.get('data'))

            # Take the fields
            user_id = data.get('user_id')
            user_name = data.get('username')    # calling the variable "username" will conflict with the global variable "username" of the DB
            pet_name = data.get('pet_name')
            pet_type = data.get('pet_type')
            date = data.get('date')
            position= data.get('position')
            description = data.get('description')

            # Get the address from starting from the coordinates
            coords = position.split(",")
            latitude = coords[0]
            longitude = coords[1]
            address = convertCoordinatesToAddress(latitude, longitude)
            address = address[:255]     # to assure that it will not be over 255 characters

            # Connect to the database
            conn = MySQLdb.connect(host=hostname, user=username, passwd=password, db=db)
            cursor = conn.cursor()

            # Take the maximum post_id found
            cursor.execute("SELECT MAX(post_id) FROM posts;")
            result = cursor.fetchone()

            # Generate a new post_id
            post_id = int(result[0]) + 1

            # Add a new row in the table
            insert_query = "INSERT INTO posts VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s);"
            values = (post_id, user_id, user_name, pet_name, pet_type, date, position, address, description)
            cursor.execute(insert_query, values)

            conn.commit()
            conn.close()

            # Save an image in the storage, in the format 'post_id.jpg'
            filepath = photoStoragePath + str(post_id) + ".jpg"
            photo = request.files['photo']
            photo.save(filepath)

            return str(post_id)


    @app.route("/userposts", methods=['GET'])
    def userposts():

        # Take the user_id from the request
        user_id = str(request.args.get("user_id"))

        # Connect to the database
        conn = MySQLdb.connect(host=hostname, user=username, passwd=password, db=db)
        cursor = conn.cursor()

        # Take all the posts of a specific user
        select_query = "SELECT * from posts where user_id='" + user_id +"';"
        cursor.execute(select_query)

        conn.commit()
        result = cursor.fetchall()
        conn.close()

        # Return as JSON
        return jsonify(result)



    @app.route("/photo", methods=['GET'])
    def photo():
        post_id = request.args.get("post_id")
        filepath = photoStoragePath + post_id + ".jpg"
        return send_file(filepath)



    @app.route("/match", methods=['GET', 'POST'])
    def match():
        # Necessary since route POST is not possible without route GET
        if request.method == 'GET':
            return "match"
        elif request.method == 'POST':

            # Take data from request
            request_user_id = str(request.form.get('user_id'))
            request_date = str(request.form.get('date'))
            request_position = str(request.form.get('position'))

            # Take latitude and longitude from position string
            coords = getLatitudeAndLongitudeFromString(request_position)
            request_latitude = coords[0]
            request_longitude = coords[1]

            # Save temporarily in the storage as 'temp.jpg'
            filepath = photoStoragePath + "temp.jpg"
            photo = request.files['photo']
            photo.save(filepath)

            # Load ML model
            model_path="/home/maccproject2024/mysite/vgg16_weights_tf_dim_ordering_tf_kernels_notop.h5"
            model = VGG16(include_top=False, weights=model_path)

            # Connect to the database
            conn = MySQLdb.connect(host=hostname, user=username, passwd=password, db=db)
            cursor = conn.cursor()

            # Take all posts
            cursor.execute("SELECT * from posts")
            conn.commit()
            all_posts = cursor.fetchall()
            conn.close()


            # Iterate over all posts and save in a list only the ones respecting maximum distances (in km and days)
            selected_posts = []

            for post in all_posts:

                # Compute the temporal distance
                # To convert a string formatted as "2023-01-28" to a datetime object
                request_dateTime = (datetime.datetime.strptime(request_date, "%Y-%m-%d")).date()
                # post[5] is the fifth column, so "date"
                distance_days = computeDateDifference(request_dateTime, post[5])

                # If the distance is over MAX_DIST_DAYS, skip the post and go to the next one
                if (distance_days > MAX_DIST_DAYS):
                    continue

                # Take latitude and longitude of the post
                # post[6] is the sixth column, so "position"
                post_coords = getLatitudeAndLongitudeFromString(post[6])
                post_latitude = post_coords[0]
                post_longitude = post_coords[1]

                # Compute the physical distance
                distance_km = computeGeographicDistance(request_latitude, request_longitude, post_latitude, post_longitude)

                # If the distance is over MAX_DIST_KM, skip the post and go to the next one
                if (distance_km > MAX_DIST_KM):
                    continue

                # Compute the similarity score, a value between 0 and 1 computed by using the ML model
                # Path of the saved photo
                input_path_1 = filepath
                # post[0] is the post_id
                input_path_2 = photoStoragePath + str(post[0]) + ".jpg"
                similarity_positive = machine_learning.similarity_between_images(model, input_path_1, input_path_2)

                # If they are not sufficiently similar, go to the next post
                if (similarity_positive < SIMILARITY_THRESHOLD):
                    continue

                similarity = 1 - similarity_positive

                #DEBUG
                #print("similarity with", str(post[0]), "is:", str(similarity), file=sys.stderr)

                # Compute a matching factor depending on similarity and on the two normalized distances (between 0 and 1)
                distance_days_normalized = distance_days / MAX_DIST_DAYS
                distance_km_normalized = distance_km / MAX_DIST_KM

                # Square the two distances to further support values near to zero
                distance_days_normalized = distance_days_normalized*distance_days_normalized
                distance_km_normalized = distance_km_normalized*distance_km_normalized

                # A value 0 for matching_factor means a "perfect" matching, an high matching_factor is not so good
                matching_factor = similarity*PHOTO_SIMILARITY_IMPORTANCE + distance_days_normalized*TEMPORAL_DISTANCE_IMPORTANCE + distance_km_normalized*PHYSICAL_DISTANCE_IMPORTANCE

                # Add the post to selected_posts (list of dictionaries), saving the computed matching factor
                selected_posts = selected_posts + [{"post":post, "matching_factor":matching_factor}]

            # Order the posts selected on the basis of maching faction and insert them in an array to return
            selected_posts = sorted(selected_posts, key=lambda d: d["matching_factor"])
            return_array = [d["post"] for d in selected_posts]

            # Return selected posts as JSON
            return jsonify(return_array)


    @app.route("/users/<user_id>/posts", methods=['GET'])
    def get_posts_of_user(user_id):

        # Connect to the database
        conn = MySQLdb.connect(host=hostname, user=username, passwd=password, db=db)
        cursor = conn.cursor()

        # Take all posts of the specified user
        select_query = "SELECT * from posts where user_id='" + user_id +"';"
        cursor.execute(select_query)

        conn.commit()
        result = cursor.fetchall()
        conn.close()

        # Return as JSON
        return jsonify(result)


    @app.route("/post/<post_id>", methods=['GET', 'DELETE'])
    def delete_post(post_id):
        if request.method == 'GET':
            return post_id
        elif request.method == 'DELETE':
            # Connect to the database
            conn = MySQLdb.connect(host=hostname, user=username, passwd=password, db=db)
            cursor = conn.cursor()

            # Delete post
            cursor.execute("DELETE from posts where post_id='" + post_id +"';")
            conn.commit()
            conn.close()

            # Delete photo
            filepath = photoStoragePath + post_id + ".jpg"
            if os.path.exists(filepath):
                os.remove(filepath)

            return post_id

    ##### Messages API ###############################################################################################

    @app.route("/messages", methods=['GET', 'POST'])
    def messages():

        # To get messages related to a specific user and chat (with reading logic)
        if request.method == 'GET':

            user_id = request.args.get('userId')
            chat_name_id = request.args.get('chatNameId')
            chat_id = int(request.args.get('chatId'))

            # Database connection
            conn = MySQLdb.connect(host=hostname, user=username, passwd=password, db=db)
            cursor = conn.cursor()

            # Check if the user requesting messages is reading the chat and in that case update the relative row
            read_query = (
                "UPDATE chats "
                "SET unread = 0 "
                "WHERE chat_id = %s "
                "AND last_receiver_id = %s "
                "AND unread = 1;"
            )

            cursor.execute(read_query, (chat_id, user_id))

            # Take all chat messages ordered by timestamp
            query = (
                "SELECT * FROM chat_messages WHERE "
                "(sender_id = %s AND receiver_id = %s) OR "
                "(sender_id = %s AND receiver_id = %s) "
                "ORDER BY timestamp;"
            )

            cursor.execute(query, (user_id, chat_name_id, chat_name_id, user_id))
            result = cursor.fetchall()

            conn.commit()
            conn.close()

            # Return result as JSON
            return jsonify(result)

        # To submit a new message
        elif request.method == 'POST':

            # Take request data
            data=json.loads(request.get_data())

            # Store the fields in variables
            message_id = int(data.get('id'))
            sender_id = data.get('senderId')
            sender_username = data.get('senderUsername')
            receiver_id = data.get('receiverId')
            receiver_username = data.get('receiverUsername')
            message = data.get('message')
            timestamp = data.get('timestamp')

            # Connect to the database
            conn = MySQLdb.connect(host=hostname, user=username, passwd=password, db=db)
            cursor = conn.cursor()

            # Retrieve the current maximum message ID
            cursor.execute("SELECT MAX(message_id) FROM chat_messages;")
            result = cursor.fetchone()

            # Generate a new message ID
            if (result[0] is not None): message_id = int(result[0]) + 1
            else: message_id = 0

            # Add the new row to the table
            insert_query = "INSERT INTO chat_messages VALUES (%s, %s, %s, %s, %s, %s, %s);"
            values = (message_id, sender_id, sender_username, receiver_id, receiver_username, message, timestamp)
            cursor.execute(insert_query, values)

            conn.commit()
            conn.close()

            return str(message_id)


     ##### Chats API ###############################################################################################

    @app.route("/chat", methods=['GET'])
    def chat():

        # To get the chat related to a specific chat ID
        chat_id = request.args.get('chatId')

        # Database connection
        conn = MySQLdb.connect(host=hostname, user=username, passwd=password, db=db)
        cursor = conn.cursor()

        print("TEST CHAT GET FOR REFRESH", file=sys.stderr)

        # Take the chat by chat ID
        query = (
            "SELECT * FROM chats WHERE "
            "chat_id = %s;"
        )

        cursor.execute(query, (chat_id,))

        conn.commit()
        result = cursor.fetchall()
        conn.close()

        print(result, file=sys.stderr)
        print(jsonify(result), file=sys.stderr)

        # Return result as JSON
        return jsonify(result)


    @app.route("/chats", methods=['GET', 'POST', 'PUT'])
    def chats():

        # To get all chats related to the user
        if request.method == 'GET':

            user_id = request.args.get('userId')

            # Database connection
            conn = MySQLdb.connect(host=hostname, user=username, passwd=password, db=db)
            cursor = conn.cursor()

            # Take all chats related to the user ordered by timestamp
            query = (
                "SELECT * FROM chats WHERE "
                "(last_sender_id = %s OR last_receiver_id = %s)"
                "ORDER BY timestamp DESC;"
            )

            cursor.execute(query, (user_id, user_id))

            conn.commit()
            result = cursor.fetchall()
            conn.close()

            # Return result as JSON
            return jsonify(result)


        # To insert a new chat or update an existing chat
        elif request.method == 'POST' or request.method == 'PUT':

            # Take request data
            data = json.loads(request.get_data())

            # Store the fields in variables
            chat_id = int(data.get('id'))
            last_sender_id = data.get('lastSenderId')
            last_sender_username = data.get('lastSenderUsername')
            last_receiver_id = data.get('lastReceiverId')
            last_receiver_username = data.get('lastReceiverUsername')
            last_message = data.get('lastMessage')
            timestamp = data.get('timestamp')
            unread = data.get('unread')

            # Connect to the database
            conn = MySQLdb.connect(host=hostname, user=username, passwd=password, db=db)
            cursor = conn.cursor()

            # Add the new row to the table or update the row
            insert_query = "INSERT INTO chats VALUES (%s, %s, %s, %s, %s, %s, %s, %s) " \
                        "ON DUPLICATE KEY UPDATE " \
                        "last_sender_id = VALUES(last_sender_id), " \
                        "last_sender_username = VALUES(last_sender_username), " \
                        "last_receiver_id = VALUES(last_receiver_id), " \
                        "last_receiver_username = VALUES(last_receiver_username), " \
                        "last_message = VALUES(last_message), " \
                        "timestamp = VALUES(timestamp), " \
                        "unread = VALUES(unread);"

            values = (chat_id, last_sender_id, last_sender_username, last_receiver_id, last_receiver_username, last_message, timestamp, unread)
            cursor.execute(insert_query, values)

            conn.commit()
            conn.close()

            return str(chat_id)


    @app.route("/notify", methods=['GET'])
    def notify():

        # To get the notify flag
        user_id = request.args.get('userId')


        # Database connection
        conn = MySQLdb.connect(host=hostname, user=username, passwd=password, db=db)
        cursor = conn.cursor()


        # Check if there is at least one chat with unread messages
        query = (
            "SELECT * FROM chats WHERE "
            "(last_receiver_id = %s AND unread = 1);"
        )

        cursor.execute(query, (user_id,))

        conn.commit()
        result = cursor.fetchall()
        ret = 0
        if result:
            ret = 1
        conn.close()

        # Return ret
        return str(ret)


api.add_resource(myHUB, '/')

if __name__ == '__main__':
    print('starting myHUB api... waiting')

    