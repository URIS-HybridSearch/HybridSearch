from typing import List
import math
import utils


class Point:
    def __init__(self, coordinates: List[float], src: str):
        self.coordinates = coordinates
        self.path = src

    def __lt__(self, other):
        # to reverse PriorityQueue's default ordering criteria
        return self.distance(utils.query_point) > other.distance(utils.query_point)

    def passes_filter(self):
        return utils.is_valid(self.path)

    def __str__(self):
        return self.path

    def distance(self, other):
        return math.sqrt(sum([(self.coordinates[i] - other.coordinates[i]) ** 2 for i in range(len(self.coordinates))]))
