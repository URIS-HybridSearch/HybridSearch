from datetime import datetime
from pathlib import Path
import time
import numpy as np
from PIL import Image
from flask import Flask, request, render_template

import point
import utils
import vptree
from feature_extractor import FeatureExtractor

app = Flask(__name__)

# Read image features
fe = FeatureExtractor()
features = []
utils.img_paths = []
cnt = 0
for feature_path in Path("./static/feature").glob("*.npy"):
    features.append(np.load(Path.__fspath__(feature_path)))
    utils.img_paths.append(Path("./static/img") / (feature_path.stem + ".jpg"))
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
        query = point.Point(coordinates=query, name="query_point")

        if utils.mode != "pre-query filtering":
            if not "full_model" in indices:
                utils.t3 = time.time()
                full_model = vptree.Vptree([point.Point(coordinates=features[i].tolist(),
                                                        name=Path.__fspath__(utils.img_paths[i])) for i in range(len(utils.img_paths))])
                utils.t4 = time.time()
                indices["full_model"] = full_model
            utils.t1 = time.time()
            result = indices["full_model"].knn_search(query, utils.k_results)
            utils.t2 = time.time()
        else:
            criteria = utils.query_text + "-" + utils.query_size
            if criteria in indices:
                utils.t4 = utils.t3 = 0
            else:
                utils.t3 = time.time()
                filtered_set = []
                for i in range(len(utils.img_paths)):
                    cap = utils.lines[Path.__fspath__(utils.img_paths[i]).replace("static\\img\\", "")]
                    if not (utils.query_text in cap):
                        continue
                    with Image.open(utils.img_paths[i]) as img:
                        width, height = img.size
                        if utils.query_size != "":
                            if str(width) + "*" + str(height) != utils.query_size:
                                continue
                    filtered_set.append(i)
                if len(filtered_set)==0:
                    return render_template('index.html', no_results=1)
                partial_model = vptree.Vptree([point.Point(coordinates=features[i].tolist(),
                                                           name=Path.__fspath__(utils.img_paths[i])) for i in filtered_set])
                utils.t4 = time.time()
                indices[criteria] = partial_model
            utils.t1 = time.time()
            result = indices[criteria].knn_search(query, utils.k_results)
            utils.t2 = time.time()

        '''

        combined_results = []
        for score, path in unstructured_scores:
            if utils.mode == "pre-query filtering":
                with Image.open(path) as img:
                    # 获取图像的宽度和高度
                    width, height = img.size
                    if utils.query_size != "":
                        if str(width) + "*" + str(height) != utils.query_size:
                            continue
            combined_results.append((score / 2, Path.__fspath__(path)))

        combined_results = sorted(combined_results, key=lambda x: x[0], reverse=True)[:100]

        http_result = []
        for result in combined_results:
            cap = utils.lines[result[1].replace("static\\img\\", "")]
            if utils.query_text in cap:
                http_result.append((cap, result[1]))
        '''
        http_result = [(utils.lines[x.name.replace("static\\img\\", "")], x.name) for x in list(result.queue)]
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
