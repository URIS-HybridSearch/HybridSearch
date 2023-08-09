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
img_paths = []
cnt = 0
for feature_path in Path("./static/feature").glob("*.npy"):
    features.append(np.load(Path.__fspath__(feature_path)))
    img_paths.append(Path("./static/img") / (feature_path.stem + ".jpg"))
    cnt += 1
    if cnt == 100:
        break
features = np.array(features)

lines = {}
with open('captions.txt', 'r') as f:
    for line in f.readlines():
        temp = line.split(',', 1)
        lines[temp[0]] = temp[1]

indices = {}
@app.route('/', methods=['GET', 'POST'])
def index():
    if request.method == 'POST':
        print(request.form)
        query_img = request.files['query_img']
        try:
            query_text = request.form['query_text']
            query_size = request.form['query_size']
        except Exception as e:
            pass
        search_type = request.form['search_type']

        utils.mode = search_type
        # Save query image
        img = Image.open(query_img.stream)  # PIL image
        uploaded_img_path = "static/uploaded/" + datetime.now().isoformat().replace(":", ".") + "_" + query_img.filename
        img.save(uploaded_img_path)

        # Run search
        query = fe.extract(img).tolist()
        query = point.Point(coordinates=query, name="query_point")

        if not "full_model" in indices:
            utils.t3 = time.time()
            full_model = vptree.Vptree([point.Point(coordinates=features[i].tolist(),
                                                    name=Path.__fspath__(img_paths[i])) for i in range(len(img_paths))])
            utils.t4 = time.time()
            indices["full_model"] = full_model
            
        utils.t1 = time.time()
        result = indices["full_model"].knn_search(query, 10)
        utils.t2 = time.time()

        '''

        combined_results = []
        for score, path in unstructured_scores:
            if utils.mode == "pre-query filtering":
                with Image.open(path) as img:
                    # 获取图像的宽度和高度
                    width, height = img.size
                    if query_size != "":
                        if str(width) + "*" + str(height) != query_size:
                            continue
            combined_results.append((score / 2, Path.__fspath__(path)))

        combined_results = sorted(combined_results, key=lambda x: x[0], reverse=True)[:100]

        http_result = []
        for result in combined_results:
            cap = lines[result[1].replace("static\\img\\", "")]
            if query_text in cap:
                http_result.append((cap, result[1]))
        '''
        http_result = [(lines[x.name.replace("static\\img\\", "")], x.name) for x in list(result.queue)]
        index_time = str(round(1000 * (utils.t4 - utils.t3)))
        if utils.mode != "pre-query filtering" and index_time != "0":
            utils.t4 = utils.t3 = 0
        return render_template('index.html',
                               query_path=uploaded_img_path,
                               scores=http_result,
                               search_time = "Query time: " + str(round(1000 * (utils.t2 - utils.t1))) + "ms",
                               indexing_time = "Indexing time: " + index_time + "ms"
                               if index_time != "0" else
                               "Index is already built.",
                               default_mode = utils.mode)
    else:
        return render_template('index.html')


if __name__ == "__main__":
    app.run("0.0.0.0")
