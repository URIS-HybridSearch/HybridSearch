import nanopq
import numpy as np
from feature_extractor import FeatureExtractor
from pathlib import Path
import os

# Set up parameters for PQ
N_subq = 16  # Number of subquantizers
N_ref = 1000  # Number of vectors to use as reference for training PQ
code_dtype = np.uint8  # Data type for PQ codes

if __name__ == '__main__':
    # Create output directory if it does not exist
    if not os.path.exists("./static/code"):
        os.makedirs("./static/code")

    # Instantiate feature extractor and PQ encoder
    fe = FeatureExtractor()
    pq = nanopq.PQ(M=N_subq, Ks=256)

    # Load images and extract features
    X_feat = []
    img_paths = []
    for i, img_path in enumerate(sorted(Path("./static/feature").glob("*.npy"))):
        feature = np.load(img_path)
        X_feat.append(feature)
        img_paths.append(Path("./static/img") / (img_path.stem + ".jpg"))
        print(f"Loaded feature {i+1}/{len(list(Path('./static/feature').glob('*.npy')))}")

    # Train PQ on a subset of the features
    X_feat_train = np.array(X_feat[:N_ref])
    pq.fit(X_feat_train)

    # Encode all features using PQ
    X_code = pq.encode(np.array(X_feat, dtype=np.float32)).astype(code_dtype)

    # Store codes and image paths in a dictionary
    code_dict = {}
    for i in range(len(img_paths)):
        code_dict[str(img_paths[i])] = X_code[i]

    # Save dictionary to a file
    np.save("./static/code/codes.npy", code_dict)
    print(f"Saved codes to disk")