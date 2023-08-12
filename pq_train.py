from sklearn.cluster import MiniBatchKMeans
import numpy as np
from scipy.cluster.vq import vq, kmeans2
from scipy.spatial.distance import cdist
from feature_extractor import FeatureExtractor
from PIL import Image
from pathlib import Path
import sys

np.set_printoptions(threshold=np.inf, linewidth=np.inf)

class ProductQuantizer:
    def __init__(self, num_clusters=256):
        self.num_clusters = num_clusters
        # self.num_subvectors = num_subvectors
        # self.codebooks = [MiniBatchKMeans(n_clusters=num_clusters) for _ in range(num_subvectors)]

    def train(self, vec, M):
        # print(vec)
        Ds = int(vec.shape[1] / M)
        codeword = np.empty((M, 256, Ds), np.float32)

        for m in range(M):
            # print(vec[m])
            print("Num ", m, " starts with vec", vec[m*Ds:(m+1)*Ds].shape, " from ", m*Ds, " to ", (m+1)*Ds)
            vec_sub = vec[:, m*Ds:(m+1)*Ds].reshape(-1, 1)
            codeword[m], label = kmeans2(vec_sub, 256, minit='random')
            print("Cluster No. ", m)
        return codeword

    def encode(self, codeword, vec):
        M, _K, Ds = codeword.shape
        pqcode = np.empty((vec.shape[0], M), np.uint8)

        for m in range(M):
            vec_sub = vec[:, m*Ds:(m+1)*Ds]
            pqcode[:m], dist = vq(vec_sub, codeword[m])

        return pqcode

    def search(self, codeword, pqcode, query):
        M, _K, Ds = codeword.shape
        dist_table = np.empty((M, 256), np.float32)

        for m in range(M):
            query_sub = query[m*Ds:(m+1)*Ds]
            dist_table[m, :] = cdist([query_sub], codeword[m], 'sqeuclidean')[0]

        dist = np.sum(dist_table[np.arange(M), pqcode], axis=1)
        return dist

if __name__ == '__main__':

    directory_path = Path("./static/code")
    directory_path.mkdir(parents=True, exist_ok=True)
    code_path = Path("./static/code") / ("codeword" + ".npy")  # e.g., ./static/feature/xxx.npy

    codeword = 0
    fe = FeatureExtractor()
    pq = ProductQuantizer(num_clusters=256)
    if not code_path.exists():
        # num_vectors = 600  # Number of vectors to concatenate

        features = []  # List to store extracted features

        for feature_path in Path("./static/feature").glob("*.npy"):
            # print(img_path)  # e.g., ./static/img/xxx.jpg

            features.append(np.load(feature_path))  # Append feature to the list
            print(len(features))
            # if len(features) == num_vectors:
            #     # Concatenate the features and obtain an array of arrays
            #     # concatenated_features = np.array(features)
            #     # print(concatenated_features.shape)
            #
            #     # Reset the features list for the next batch
            #     # features = []
            #     break

        print("size: ", np.array(features).shape)

        codeword = pq.train(np.array(features), 4)
        np.save(code_path, codeword)

    else:
        codeword = np.load(code_path)

    print(codeword.shape)

    test_img_vec = fe.extract(img=Image.open("./test_img.jpg"))


    for feature_path in Path("./static/feature").glob("*.npy"):
        vec = []
        vec.append(np.load(feature_path))
        pqcode = pq.encode(codeword, np.array(vec))
        dist = pq.search(codeword, pqcode, test_img_vec)
        if dist<0.95:
            print(feature_path, pqcode, dist)


    # Save quantized feature to disk
    feature_path = Path("./static/code") / ("all_code" + ".npy")  # e.g., ./static/feature/xxx.npy

    # print("feature:", quantized_feature[0])
    # np.save(feature_path, quantized_feature)