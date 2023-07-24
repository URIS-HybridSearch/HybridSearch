import numpy as np
from PIL import Image
from feature_extractor import FeatureExtractor
from datetime import datetime
from flask import Flask, request, render_template
from pathlib import Path
import nltk
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
from sklearn.feature_extraction.text import TfidfVectorizer
import pickle

# download stopwords and punkt
nltk.download('stopwords')
nltk.download('punkt')

app = Flask(__name__)

# Read image features
fe = FeatureExtractor()
features = []
img_paths = []
for feature_path in Path("./static/feature").glob("*.npy"):
    features.append(np.load(feature_path))
    img_paths.append(Path("./static/img") / (feature_path.stem + ".jpg"))
features = np.array(features)


@app.route('/', methods=['GET', 'POST'])
def index():
    if request.method == 'POST':
        query_img = request.files['query_img']
        query_text = request.form['query_text']

        # Save query image
        img = Image.open(query_img.stream)  # PIL image
        uploaded_img_path = "static/uploaded/" + datetime.now().isoformat().replace(":", ".") + "_" + query_img.filename
        img.save(uploaded_img_path)

        # Run search
        query = fe.extract(img)
        dists = np.linalg.norm(features-query, axis=1)  # L2 distances to features
        ids = np.argsort(dists)[:300]  # Top 30 results
        unstructured_scores = [(dists[id], img_paths[id]) for id in ids]

        # Search captions and get top n_results
        structured_results = search_captions(query_text)
        n_results = 50
        structured_results = structured_results[:n_results]

        # Combine structured and unstructured results and sort by score
        combined_results = []
        for score, path in unstructured_scores:
            combined_results.append((score, path))
        for caption in structured_results:
            combined_results.append((1.0, caption))
        combined_results = sorted(combined_results, key=lambda x: x[0], reverse=True)[:10]

        return render_template('index.html',
                               query_path=uploaded_img_path,
                               scores=combined_results)
    else:
        return render_template('index.html')

# define a function to search captions
def search_captions(query, vectorizer_path='tfidf_vectorizer.pkl', matrix_path='tfidf_matrix.pkl', n_results=50):
    # load captions from file
    with open('captions.txt', 'r') as f:
        captions = f.readlines()
    # load the vectorizer and the matrix from disk
    with open(vectorizer_path, 'rb') as f:
        vectorizer = pickle.load(f)

    with open(matrix_path, 'rb') as f:
        tfidf_matrix = pickle.load(f)

    # preprocess the query
    query = query.lower()
    words = word_tokenize(query)
    stop_words = set(stopwords.words('english'))
    words = [word for word in words if word.isalnum() and word not in stop_words]
    query = ' '.join(words)

    # compute the TF-IDF vector for the query
    query_tfidf = vectorizer.transform([query])

    # compute cosine similarity between query vector and all captions
    similarities = np.dot(tfidf_matrix, query_tfidf.T).toarray().flatten()

    # rank captions based on similarity scores
    ranked_indices = np.argsort(similarities)[::-1][:n_results]

    # return the top n_results captions
    results = []
    for idx in ranked_indices:
        results.append(captions[idx])
    return results

if __name__=="__main__":
    app.run("0.0.0.0")
