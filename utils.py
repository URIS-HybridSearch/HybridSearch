from pathlib import Path
from PIL import Image

# global variables

maxheap = None

knn_threshold = None

query_point = None

query_img = None

query_text = None

query_size = None

lines = None

t1 = t2 = t3 = t4 = 0

# number of results wanted
k_results = 10

# options: "no filtering", "pre-query filtering", "post-query filtering", "concurrent filtering"
mode = None

def is_valid(img_path):
    if mode != "no filtering":
        cap = lines[Path.__fspath__(img_path).replace("static\\img\\", "")]
        if not (query_text in cap):
            return False
        if query_size != "":
            with Image.open(img_path) as img:
                width, height = img.size
                if str(width) + "*" + str(height) != query_size:
                    return False
        return True
    else:
        return True