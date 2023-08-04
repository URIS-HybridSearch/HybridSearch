import time

import utils
from point import Point
from vptree import Vptree

if __name__ == "__main__":
    a = []
    try:
        with open(utils.input_path, "r", encoding="UTF-8") as f:
            for i in range(10000):
                line = f.readline()
                if not line:
                    break
                values = line.strip().split()
                point_name = values[0]
                coordinates = [float(c) for c in values[1:]]
                point = Point(coordinates, point_name)
                a.append(point)
                if (i + 1) % 10000 == 0:
                    print(f"{i + 1} points have been read.")
    except FileNotFoundError:
        print("File not found")
        exit()
    print("Finished reading.\n")

    if utils.mode == "pre-query filtering":
        a = [x for x in a if x.passes_filter()]

    vptree = Vptree(a)

    while True:
        k = utils.k_results
        coordinates = []
        utils.maxheap = None
        utils.knn_threshold = float("inf")
        print("Enter a point:")
        try:
            coordinates = input().split(" ")
            coordinates = [float(x) for x in coordinates]
        except KeyboardInterrupt:
            break
        except Exception as e:
            print(e)
            break

        utils.query_point = Point(coordinates, "Query Point")
        t1 = int(round(time.time() * 1000))
        results = vptree.knn_search(utils.query_point, k if utils.mode != "post-query filtering" else round(k / utils.filter_coefficient))
        t2 = int(round(time.time() * 1000))
        if utils.mode == "post-query filtering":
            results = [x for x in results.queue if x.passes_filter()]
            print(f"The nearest {len(results)} are: ")
            for i in list(results):
                print(i)
            print(f"Time used: {t2 - t1}ms")
        else:
            print(f"The nearest {k} are: ")
            for i in list(results.queue):
                print(i)
            print(f"Time used: {t2 - t1}ms")