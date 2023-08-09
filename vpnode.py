from queue import PriorityQueue
import utils


class VpNode:
    def __init__(self, data):
        self.lc = None
        self.rc = None
        self.content = data
        self.radius = -1

    def knn_search(self, query_point, k):

        # print(self.content)
        # print("maxheap: ", [x.name for x in list(utils.maxheap.queue)])
        # print("knn_threshold: ", utils.knn_threshold)

        if utils.maxheap is None:
            utils.maxheap = PriorityQueue()
        if self.content is None:
            return
        current_distance = self.content.distance(query_point)
        if current_distance <= utils.knn_threshold:
            if utils.mode == "concurrent filtering":
                if self.content.passes_filter():
                    utils.maxheap.put(self.content)
                    if utils.maxheap.qsize() > k:
                        utils.maxheap.get()
                        utils.knn_threshold = utils.maxheap.queue[0].distance(query_point)
            else:
                utils.maxheap.put(self.content)
                if utils.maxheap.qsize() > k:
                    utils.maxheap.get()
                    utils.knn_threshold = utils.maxheap.queue[0].distance(query_point)
        if self.lc is not None:
            if current_distance <= self.radius + utils.knn_threshold:
                self.lc.knn_search(query_point, k)

        if self.rc is not None:
            if current_distance >= self.radius - utils.knn_threshold:
                self.rc.knn_search(query_point, k)
