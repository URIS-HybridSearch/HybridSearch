from sklearn.cluster import MiniBatchKMeans
import numpy as np
from scipy.cluster.vq import vq, kmeans2
from scipy.spatial.distance import cdist
from feature_extractor import FeatureExtractor
from PIL import Image
from pathlib import Path

np.set_printoptions(threshold=np.inf, linewidth=np.inf)

class ProductQuantizer:
    def __init__(self, num_clusters=256, num_subvectors=4):
        self.num_clusters = num_clusters
        # self.num_subvectors = num_subvectors
        # self.codebooks = [MiniBatchKMeans(n_clusters=num_clusters) for _ in range(num_subvectors)]

    def train(self, vec, M):
        # print(vec)
        Ds = int(vec.shape[0] / M)
        codeword = np.empty((M, 256, Ds), np.float32)

        for m in range(M):
            # print(vec[m])
            vec_sub = vec[m*Ds:(m+1)*Ds].reshape(-1, 1)
            codeword[m], label = kmeans2(vec_sub, 256)
            print("codeword[m]: ", codeword[m].shape)
            print("label: ",label.shape)

        return codeword


if __name__ == '__main__':
    fe = FeatureExtractor()
    pq = ProductQuantizer(num_clusters=256, num_subvectors=4)

    for i, img_path in enumerate(sorted(Path("./static/img").glob("*.jpg"))):
        print(img_path)  # e.g., ./static/img/xxx.jpg

        # Extract deep feature from image
        feature = fe.extract(img=Image.open(img_path))
        # print(feature)
        # assert 1==2
        print("Feature shape:", feature.shape)

        # Quantize deep feature using product quantization
        quantized_feature = pq.train(feature, 4)


        # Save quantized feature to disk
        # feature_path = Path("./static/code") / (img_path.stem + ".npy")  # e.g., ./static/feature/xxx.npy
        # np.save(feature_path, quantized_feature)