import random
from queue import PriorityQueue
from vpnode import VpNode
import utils


class Vptree:
    def __init__(self, data):
        self.root = self.build_tree(data)

    def build_tree(self, points):
        # print([str(x) for x in points])
        if len(points) == 1:
            return VpNode(points[0])
        if len(points) == 0:
            return None
        rand = random.Random()
        vp_id = rand.randint(0, len(points) - 1)
        vp = points[vp_id]
        ans = VpNode(vp)
        # print("VP: "+str(vp))
        points.pop(vp_id)
        points.sort(key=lambda x: ans.content.distance(x))
        inner = []
        outer = []
        if len(points) >= 2:
            ans.radius = (points[(len(points)-1) // 2].distance(vp) + points[len(points) // 2].distance(vp)) / 2
        else:
            ans.radius = 0
        # print("radius: ", ans.radius)
        for point in points:
            if point.distance(vp) <= ans.radius:
                inner.append(point)
            else:
                outer.append(point)
        ans.lc = self.build_tree(inner)
        ans.rc = self.build_tree(outer)
        return ans

    utils.maxheap = None
    utils.knn_threshold = float("inf")

    def knn_search(self, query_point, k):
        utils.query_point = query_point
        if utils.maxheap is None:
            utils.maxheap = PriorityQueue()
        self.root.knn_search(query_point, k)
        try:
            return utils.maxheap
        finally:
            utils.maxheap = None
            utils.knn_threshold = float("inf")
