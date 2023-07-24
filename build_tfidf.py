import nltk
from nltk.corpus import stopwords
from nltk.tokenize import word_tokenize
from sklearn.feature_extraction.text import TfidfVectorizer
import numpy as np
import pickle

# download stopwords and punkt
nltk.download('stopwords')
nltk.download('punkt')

# load captions from file
with open('captions.txt', 'r') as f:
    captions = f.readlines()

# preprocess the captions
processed_captions = []
stop_words = set(stopwords.words('english'))
for caption in captions:
    # convert to lowercase
    caption = caption.lower()
    # tokenize the caption
    words = word_tokenize(caption)
    # remove stopwords and punctuation
    words = [word for word in words if word.isalnum() and word not in stop_words]
    # join the words back to form a caption
    processed_caption = ' '.join(words)
    processed_captions.append(processed_caption)

# create a TF-IDF vectorizer
vectorizer = TfidfVectorizer()

# fit the vectorizer on the processed captions
tfidf_matrix = vectorizer.fit_transform(processed_captions)

# save the vectorizer and the matrix to disk
with open('tfidf_vectorizer.pkl', 'wb') as f:
    pickle.dump(vectorizer, f)
with open('tfidf_matrix.pkl', 'wb') as f:
    pickle.dump(tfidf_matrix, f)


# define a function to search captions
def search_captions(query, vectorizer_path='tfidf_vectorizer.pkl', matrix_path='tfidf_matrix.pkl', n_results=10):
    # load the vectorizer and the matrix from disk
    with open(vectorizer_path, 'rb') as f:
        vectorizer = pickle.load(f)
    with open(matrix_path, 'rb') as f:
        tfidf_matrix = pickle.load(f)

    # preprocess the query
    query = query.lower()
    words = word_tokenize(query)
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


# example usage
query = "sing"
results = search_captions(query, vectorizer_path='tfidf_vectorizer.pkl', matrix_path='tfidf_matrix.pkl', n_results=5)
for result in results:
    print(result.strip())