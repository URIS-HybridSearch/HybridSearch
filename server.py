import nanopq
import numpy as np
from PIL import Image
from feature_extractor import FeatureExtractor
from datetime import datetime
from flask import Flask, request, render_template
from pathlib import Path
import nltk
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
import pickle
import logging
import json

# download stopwords and punkt
nltk.download('stopwords')
nltk.download('punkt')

app = Flask(__name__)

# Set up logging
logging.basicConfig(filename='app.log', level=logging.DEBUG)

# Read image features
fe = FeatureExtractor()
features = []
img_paths = []
for feature_path in Path("./static/feature").glob("*.npy"):
    features.append(np.load(feature_path))
    img_paths.append(Path("./static/img") / (feature_path.stem + ".jpg"))
features = np.array(features)

# Set up parameters for PQ
N_subq = 16  # Number of subquantizers
N_ref = 1000  # Number of vectors to use as reference for training PQ
code_dtype = np.uint8  # Data type for PQ codes

# Create an empty dictionary to store the captions
caption_dict = {}

# Load the dictionary from the JSON file
with open('captions.json', 'r') as f:
    caption_dict = json.load(f)

# Read the "captions.txt" file
with open('captions.txt', 'r') as captions_file:
    lines = captions_file.readlines()


@app.route('/', methods=['GET', 'POST'])
def index():

    if request.method == 'POST':
        query_img = request.files['query_img']
        query_text = request.form['query_text']
        query_size = request.form['query_size']
        search_type = request.form['search_type']

        # Save query image in the folder.
        img = Image.open(query_img.stream)  # PIL image
        uploaded_img_path = "static/uploaded/" + datetime.now().isoformat().replace(":", ".") + "_" + query_img.filename
        img.save(uploaded_img_path)

        # normal hybrid search
        if search_type == "normal":
            # Run search
            query = fe.extract(img)
            dists = np.linalg.norm(features - query, axis=1)  # L2 distances to features
            ids = np.argsort(dists)[:300]
            unstructured_scores = [(dists[id], img_paths[id]) for id in ids]

            # Combine structured and unstructured results and sort by score
            combined_results = []
            for score, path in unstructured_scores:
                print(path)
                with Image.open(path) as img:
                    # 获取图像的宽度和高度
                    width, height = img.size
                    if query_size != "":
                        if str(width) + "*" + str(height) != query_size:
                            continue
                combined_results.append((score / 2, Path.__fspath__(path)))

            combined_results = sorted(combined_results, key=lambda x: x[0], reverse=True)[:200]

            http_result = []
            for result in combined_results:
                caps = caption_dict[result[1].replace("static\\img\\", "")]
                for cap in caps:
                    if query_text in cap:
                        http_result.append((cap, result[1]))
                        break

            return render_template('index.html',
                                   query_path=uploaded_img_path,
                                   scores=http_result)

        elif search_type == 'tfidf':
            # Load the TF-IDF vectorizer and the TF-IDF matrix from disk
            with open('tfidf_vectorizer.pickle', 'rb') as f:
                vectorizer = pickle.load(f)
            with open('tfidf_matrix.pickle', 'rb') as f:
                tfidf_matrix = pickle.load(f)

            # Preprocess the query text
            query_text = query_text.lower()
            # print(query_text)
            words = word_tokenize(query_text)
            stop_words = set(stopwords.words('english'))
            words = [word for word in words if word.isalnum() and word not in stop_words]
            query_text = ' '.join(words)

            # Preprocess user input
            user_input_vector = vectorizer.transform([query_text])

            # Compute cosine similarities between user input and captions
            similarities = np.dot(tfidf_matrix, user_input_vector.T).toarray().flatten()

            # Rank captions based on similarity scores
            ranked_indices = np.argsort(similarities)[::-1][:100]

            unique_filenames = set()

            # Retrieve the top 500 image filenames and captions
            tfidf_results = []
            for idx in ranked_indices:

                image_filename = lines[idx].split(',')[0]
                if image_filename in unique_filenames:
                    continue
                caption = lines[idx].split(',')[1]
                similarities_score = similarities[idx]
                unique_filenames.add(image_filename)
                tfidf_results.append(((Path("./static/img") / image_filename), caption, similarities_score))
                # print(Path("./static/img") / image_filename, caption)

            print("============================")
            # Run search for images
            query = fe.extract(img)
            dists = np.linalg.norm(features - query, axis=1)  # L2 distances to features
            ids = np.argsort(dists)[:100]
            unstructured_result = [(dists[id], img_paths[id]) for id in ids]

            # test: Iterate through unstructured_result
            # for score, img_path in unstructured_result:
                # Access the distance and image path for each element
                # print("Distance:", score)
                # print("Image Path:", img_path)
                # Perform further processing or operations with the distance and image path

            # Find the intersection of image filenames in tfidf_results and img_path in unstructured_result
            intersection = []

            for filename, caption, score in tfidf_results:
                intersection.append((filename, caption, score))
                # print("tfidf score:", score)

            for score, filename in unstructured_result:
                # print("image score: ", score)
                for item in intersection:
                    if item[0] == filename:
                        item = (item[0], item[1], item[2] + score / 2)
                        break
                else:
                    intersection.append((filename, caption_dict[filename.name][0], score / 2))


            combined_results = sorted(intersection, key=lambda x: x[2], reverse=True)[:50]
            for image_filename, a, b in intersection:
                # Access the image filename and perform further processing or operations
                print("Common Image Filename:", image_filename, a, b)
            # combined_results = [(caption, image_filename) for image_filename, caption, _ in combined_results]

            http_results = []
            for image_filename, caption, score in combined_results:
                with Image.open(image_filename) as img:
                    width, height = img.size
                    if query_size != "":
                        if str(width) + "*" + str(height) != query_size:
                            continue
                http_results.append((caption, image_filename))
                # print("http result: ", caption, image_filename, score)


            return render_template('index.html',
                                   query_path=uploaded_img_path,
                                   scores=http_results)

        # product quantization
        elif search_type == 'pq':
            pq = nanopq.PQ(M=N_subq, Ks=256)
            # Load PQ codes from codes.npy file
            try:
                codes_dict = np.load('./static/code/codes.npy', allow_pickle=True).item()
            except Exception as e:
                logging.error(f"Error loading PQ codes: {e}")
                return render_template('index.html', error="Error loading PQ codes from file.")

            logging.debug("A")
            # Create a new dictionary to store PQ codes along with image paths
            # key: path of the image
            # value: product quantization code.
            X_code_dict = {}
            for key, value in codes_dict.items():
                X_code_dict[Path(key)] = value

            logging.debug("B")
            X_code = list(X_code_dict.values())

            # Extract features from query image and encode using PQ
            query = fe.extract(img)
            query_code = pq.encode(np.array(query, dtype=np.float32)).astype(code_dtype)

            logging.debug("C")

            # Compute distances between query code and indexed codes
            unstructured_scores = pq.dtable(query_code).adist(X_code)
            logging.log(unstructured_scores)

            # Combine structured and unstructured results and sort by score
            combined_results = []
            for score, path in unstructured_scores:
                with Image.open(path) as img:
                    # 获取图像的宽度和高度
                    width, height = img.size
                    if query_size != "":
                        if str(width) + "*" + str(height) != query_size:
                            continue
                combined_results.append((score / 2, Path.__fspath__(path)))

            # Log search results
            for score, path in unstructured_scores:
                logging.debug(f"Score: {score:.2f}, Path: {path}")

            # Display search results
            http_result = []
            for result in combined_results:
                cap = lines[result[1].replace("static\\img\\", "")]
                if query_text in cap:
                    http_result.append((cap, result[1]))

            return render_template('index.html',
                                   query_path=uploaded_img_path,
                                   scores=http_result)

        return render_template('index.html')

    else:
        return render_template('index.html')


if __name__ == "__main__":

    # Get the number of key-value pairs in the lines dictionary
    num_pairs = len(caption_dict)

    print("The lines dictionary contains", num_pairs, "key-value pairs.")

    print("Open the web on http://localhost:5000")
    app.run("0.0.0.0")