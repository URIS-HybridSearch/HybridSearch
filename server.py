from datetime import datetime
from pathlib import Path
import time
import numpy as np
from PIL import Image
from flask import Flask, request, render_template
import random
import point
import utils
import vptree
from feature_extractor import FeatureExtractor

app = Flask(__name__)

# Read image features
fe = FeatureExtractor()
features = []
img_paths = []
cnt = 0
for feature_path in Path("./static/feature").glob("*.npy"):
    features.append(np.load(Path.__fspath__(feature_path)))
    img_paths.append(Path("./static/img") / (feature_path.stem + ".jpg"))
    cnt += 1
    if cnt == 100:
        break
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
        utils.query_size = request.form.get('query_size')
        utils.mode = request.form.get('search_type')

        if not utils.query_text and not utils.query_size:
            utils.mode = "no filtering"
        # Save query image
        img = Image.open(utils.query_img.stream)  # PIL image
        uploaded_img_path = "static/uploaded/" + datetime.now().isoformat().replace(":", ".") + "_" + utils.query_img.filename
        img.save(uploaded_img_path)

        # Run search
        query = fe.extract(img).tolist()
        query = point.Point(coordinates=query, src="query_point")

        if utils.mode != "pre-query filtering":
            if not "full_model" in indices:
                utils.t3 = time.time()
                full_model = vptree.Vptree([point.Point(coordinates=features[i].tolist(),
                                                        src=Path.__fspath__(img_paths[i])) for i in range(len(img_paths))])
                utils.t4 = time.time()
                indices["full_model"] = full_model
            utils.t1 = time.time()
            if utils.mode != "post-query filtering":
                result = indices["full_model"].knn_search(query, utils.k_results)
            else:
                count = 0
                length = len(img_paths)
                for i in range(50):
                    if utils.is_valid(img_paths[random.randint(0,length-1)]):
                        count += 1
                p = float(count) / 50
                search_k = length if utils.k_results / p >= length else round(utils.k_results / p)
                result = indices["full_model"].knn_search(query, search_k)
                result.queue = [x for x in result.queue if x.passes_filter()]
            utils.t2 = time.time()
        else:
            criteria = utils.query_text + "-" + utils.query_size
            if criteria in indices:
                utils.t4 = utils.t3 = 0
            else:
                utils.t3 = time.time()
                filtered_set = []
                for i in range(len(img_paths)):
                    if utils.is_valid(img_paths[i]):
                        filtered_set.append(i)
                if len(filtered_set)==0:
                    return render_template('index.html', no_results=1)
                partial_model = vptree.Vptree([point.Point(coordinates=features[i].tolist(),
                                                           src=Path.__fspath__(img_paths[i])) for i in filtered_set])
                utils.t4 = time.time()
                indices[criteria] = partial_model
            utils.t1 = time.time()
            result = indices[criteria].knn_search(query, utils.k_results)
            utils.t2 = time.time()

        http_result = [(utils.lines[x.path.replace("static\\img\\", "")], x.path) for x in list(result.queue)]
        index_time = str(round(1000 * (utils.t4 - utils.t3)))
        if utils.mode != "pre-query filtering" and index_time != "0":
            utils.t4 = utils.t3 = 0
        return render_template('index.html',
                               query_path=uploaded_img_path,
                               scores=http_result,
                               search_time="Query time: " + str(round(1000 * (utils.t2 - utils.t1))) + "ms",
                               indexing_time="Indexing time: " + index_time + "ms"
                               if index_time != "0" else
                               "Index is already built.",
                               default_mode=utils.mode,
                               no_results=0)
    else:
        return render_template('index.html', no_results=0)


if __name__ == "__main__":
    app.run("0.0.0.0")
