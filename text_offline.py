import json
import pickle
from sklearn.feature_extraction.text import TfidfVectorizer

# Create an empty dictionary to store the captions
caption_dict = {}

# Open the captions.txt file and read its lines
with open('captions.txt', 'r') as f:
    for line in f.readlines():
        # Split each line into an image filename and a caption
        image_filename, caption = line.strip().split(',', 1)

        # If the image filename is already a key in the dictionary, add the caption to its list of captions
        if image_filename in caption_dict:
            caption_dict[image_filename].append(caption)
        # Otherwise, create a new key-value pair with the image filename and a list containing the caption
        else:
            caption_dict[image_filename] = [caption]

# Save the dictionary to a JSON file
with open('captions.json', 'w') as f:
    json.dump(caption_dict, f)

# build tfidf.
# Step 1: Parse the JSON file
with open('captions.json') as json_file:
    data = json.load(json_file)

# Step 2: Preprocess the text data
corpus = []
for key, values in data.items():
    if key != "image":
        corpus.extend(values)

# Step 3: Calculate TF-IDF
vectorizer = TfidfVectorizer()
tfidf_matrix = vectorizer.fit_transform(corpus)

# Save the TF-IDF matrix and vectorizer to files
with open('tfidf_matrix.pickle', 'wb') as matrix_file:
    pickle.dump(tfidf_matrix, matrix_file)

with open('tfidf_vectorizer.pickle', 'wb') as vectorizer_file:
    pickle.dump(vectorizer, vectorizer_file)