from datetime import datetime
from pathlib import Path
import time
from datetime import datetime
import numpy as np
from PIL import Image
from flask import Flask, request, render_template
import random
import point
import utils
import heapq
from product_quantization import ProductQuantizer
import vptree
from feature_extractor import FeatureExtractor

app = Flask(__name__)

# Read image features
fe = FeatureExtractor()
pq = ProductQuantizer(num_clusters=256)

features = []
img_paths = []
for feature_path in Path("./static/feature").glob("*.npy"):
    features.append(np.load(Path.__fspath__(feature_path)))
    img_paths.append(Path("./static/img") / (feature_path.stem + ".jpg"))
features = np.array(features)

utils.lines = {}
with open('captions.txt', 'r') as f:
    for line in f.readlines():
        temp = line.split(',', 1)
        utils.lines[temp[0]] = temp[1]

indices = {}


@app.route('/', methods=['GET', 'POST'])
def index():
    if request.method == 'POST':
        utils.query_img = request.files.get('query_img')
        utils.query_text = request.form.get('query_text')
        if utils.query_text is None:
            utils.query_text = ""
        utils.query_size = request.form.get('query_size')
        if utils.query_size is None:
            utils.query_size = "any"
        utils.query_anns = request.form.get('anns_method')
        # Split the string based on spaces
        split_string = utils.query_anns.split()
        # Extract the string part
        string_part = " ".join(split_string[:-1])
        pq_k = 16
        if string_part == "product quantization":
            utils.query_anns = string_part
            # Extract the integer part
            pq_k = int(split_string[-1])

        if utils.query_anns is None:
            utils.query_size = "VP-Tree"
        utils.mode = request.form.get('search_type')
        utils.database_size = int(request.form.get("database_size"))
        utils.k_results = int(request.form.get("num_results"))
        if not utils.query_text and utils.query_size == "any":
            utils.mode = "no filtering"

        # Save query image
        img = Image.open(utils.query_img.stream)  # PIL image
        uploaded_img_path = "static/uploaded/" + datetime.now()\
            .isoformat().replace(":", ".") + "_" + utils.query_img.filename
        img.save(uploaded_img_path)

        # PQ
        if utils.query_anns == "product quantization":

            pq_path = Path("./static/code")
            pq_path.mkdir(parents=True, exist_ok=True)
            code_path = Path("./static/code") / ("codeword_" + str(pq_k) + ".npy")  # e.g., ./static/feature/xxx.npy
            dict_path = Path("./static/code") / ("dict_" + str(pq_k) + ".npy")

            query = fe.extract(img)

            if not code_path.exists():
                # codebook construction
                # Number of vectors to concatenate
                features_list = []  # List to store extracted features
                # the codeword is trained already and provided, so this training can be ignored.
                for feature_path in Path("./static/pq_feature_train").glob("*.npy"):
                    # print(img_path)  # e.g., ./static/img/xxx.jpg
                    features_list.append(np.load(feature_path))  # Append feature to the list
                    print(len(features_list))

                for feature_path in Path("./static/pq_feature_train2").glob("*.npy"):
                    # print(img_path)  # e.g., ./static/img/xxx.jpg
                    features_list.append(np.load(feature_path))  # Append feature to the list
                    print(len(features_list))

                print("size: ", np.array(features_list).shape)

                codeword = pq.train(np.array(features_list), pq_k)
                np.save(code_path, codeword)
            else:
                codeword = np.load(code_path)

            utils.t1 = time.time()
            print("The shape of codeword: ", codeword.shape)

            min_score_paths = []

            file_code_dict = {}

            if not dict_path.exists():
                # file_code_dict = {}
                for feature_path in Path("./static/feature").glob("*.npy"):
                    vec = []
                    vec.append(np.load(feature_path))
                    pqcode = pq.encode(codeword, np.array(vec))
                    filename_without_suffix = feature_path.stem
                    # print(filename_without_suffix)
                    file_code_dict[filename_without_suffix] = pqcode
                    np.save(dict_path, file_code_dict)
                    # print(file_code_dict)
            else:
                file_code_dict = np.load(dict_path, allow_pickle=True).item()

            print("1. Current time is:", datetime.now())
            pqdistance_filename_dict = {}
            counter = 0
            for filename, pqcode in file_code_dict.items():
                dist = pq.search(codeword, pqcode, query)
                dist = dist[0]
                if dist in pqdistance_filename_dict:
                    pqdistance_filename_dict[dist].append(filename)
                else:
                    pqdistance_filename_dict[dist] = [filename]
                counter += 1
                if counter == utils.database_size:
                    break
            print("2. Current time is:", datetime.now())
            # Get the minimum distances using heapq
            smallest_distances = heapq.nsmallest(5, pqdistance_filename_dict.keys())
            # print(smallest_distances)
            # Retrieve filenames corresponding to the smallest distances
            filenames = [filename for distance, filenames in pqdistance_filename_dict.items() if
                         distance in smallest_distances for filename in filenames]
            print("3. Current time is:", datetime.now())
            # Print the filenames
            # print(filenames)
            for filename in filenames:
                vec = np.load(Path("./static/feature") / (filename + ".npy"))
                real_dist = np.linalg.norm(query - vec)  # Calculate the real distance

                if utils.is_valid(Path("./static/img") / (filename + ".jpg")):
                    min_score_paths.append((filename, real_dist))

            # Rank the paths in ascending order based on real distance
            min_score_paths.sort(key=lambda x: x[1])
            min_score_paths = min_score_paths[:utils.k_results]

            http_result = []

            for filename, real_dist in min_score_paths:
                converted_path = "static/img/" + filename + ".jpg"
                converted_filename = filename + ".jpg"
                http_result.append((utils.lines[converted_filename], converted_path))

            utils.t2 = time.time()

            return render_template('index.html',
                                   query_path=uploaded_img_path,
                                   scores=http_result,
                                   search_time="Query time: " + str(round(1000 * (utils.t2 - utils.t1))) + "ms",
                                   default_text=utils.query_text,
                                   default_size=utils.query_size,
                                   default_mode=utils.mode,
                                   default_database_size=utils.database_size,
                                   default_num_results=utils.k_results,
                                   num_results=str(len(http_result)))

        # vp-tree
        else:
            # Run search
            query = fe.extract(img).tolist()
            query = point.Point(coordinates=query, src="query_point")

            if utils.mode != "pre-query filtering":
                full_model_name = "full_model" + "_" + str(utils.database_size)
                if full_model_name not in indices:
                    utils.t3 = time.time()
                    full_model = vptree.Vptree([point.Point(coordinates=features[i].tolist(),
                                                            src=Path.__fspath__(img_paths[i])) for i in
                                                range(utils.database_size)])
                    utils.t4 = time.time()
                    indices[full_model_name] = full_model
                else:
                    utils.t4 = utils.t3 = 0
                utils.t1 = time.time()
                if utils.mode != "post-query filtering":
                    result = indices[full_model_name].knn_search(query, utils.k_results)
                else:
                    count = 0
                    for i in range(50):
                        if utils.is_valid(img_paths[random.randint(0, utils.database_size - 1)]):
                            count += 1
                    search_k = utils.database_size
                    if count != 0:
                        p = float(count) / 50
                        search_k = min(utils.k_results / p, utils.database_size)
                    result = indices[full_model_name].knn_search(query, search_k)
                    result.queue = [x for x in result.queue if x.passes_filter()]
                utils.t2 = time.time()
            else:
                criteria = utils.query_text + "_" + utils.query_size + "_" + str(utils.database_size)
                if criteria in indices:
                    utils.t4 = utils.t3 = 0
                else:
                    utils.t3 = time.time()
                    filtered_set = []
                    for i in range(utils.database_size):
                        if utils.is_valid(img_paths[i]):
                            filtered_set.append(i)
                    if len(filtered_set) == 0:
                        return render_template('index.html',
                                               num_results="0",
                                               default_text=utils.query_text,
                                               default_size=utils.query_size,
                                               default_mode=utils.mode,
                                               default_database_size=utils.database_size,
                                               default_num_results=utils.k_results)
                    partial_model = vptree.Vptree([point.Point(coordinates=features[i].tolist(),
                                                               src=Path.__fspath__(img_paths[i])) for i in filtered_set])
                    utils.t4 = time.time()
                    indices[criteria] = partial_model
                utils.t1 = time.time()
                result = indices[criteria].knn_search(query, utils.k_results)
                utils.t2 = time.time()
            print(result.queue)
            http_result = [(utils.lines[x.path.replace("static\\img\\", "")], x.path)
                           for x in sorted(list(result.queue), reverse=True)]
            print(http_result)
            index_time = str(round(1000 * (utils.t4 - utils.t3)))
            return render_template('index.html',
                                   query_path=uploaded_img_path,
                                   scores=http_result,
                                   search_time="Query time: " + str(round(1000 * (utils.t2 - utils.t1))) + "ms",
                                   indexing_time="Indexing time: " + index_time + "ms"
                                   if index_time != "0" else
                                   "Index is already built.",
                                   default_text=utils.query_text,
                                   default_size=utils.query_size,
                                   default_mode=utils.mode,
                                   default_database_size=utils.database_size,
                                   default_num_results=utils.k_results,
                                   num_results=str(len(http_result)))
    else:
        return render_template('index.html', num_results="",
                               default_mode="no filtering",
                               default_text="",
                               default_size="any",
                               default_database_size=100,
                               default_num_results=10)


if __name__ == "__main__":
    app.run("0.0.0.0")
