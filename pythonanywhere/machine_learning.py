from tensorflow.keras.preprocessing import image
from tensorflow.keras.applications.vgg16 import preprocess_input

import numpy as np
# For debug purposes
import sys

from sklearn.metrics.pairwise import cosine_similarity


# Function to preprocess the input image
def preprocess_image(img):
    img_array = image.img_to_array(img)
    img_array = np.expand_dims(img_array, axis=0)
    img_array = preprocess_input(img_array)

    return img_array

# Function to extract features from the input image
def extract_features(model, img):
    img_array = preprocess_image(img)
    features = model.predict(img_array, verbose=0)
    # Flatten the features to a 1D array
    features = features.flatten()

    return features

# Function to calculate cosine similarity between two feature vectors
def similarity_score(features1, features2):
    features1 = np.reshape(features1, (1, -1))
    features2 = np.reshape(features2, (1, -1))

    return cosine_similarity(features1, features2)[0][0]


# Image similarity between a pair of images
def similarity_between_images(model, input_path_1, input_path_2):

    # Load images
    input_img_1 = image.load_img(input_path_1, target_size=(224, 224))
    input_img_2 = image.load_img(input_path_2, target_size=(224, 224))

    # Extract features from images
    features_1 = extract_features(model, input_img_1)
    features_2 = extract_features(model, input_img_2)

    # Compute similarity between features
    similarity = similarity_score(features_1, features_2)

    return similarity


if __name__ == '__main__':
    print("main function of machine_learning.py")

