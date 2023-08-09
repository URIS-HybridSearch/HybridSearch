from typing import List
import math
import utils


class Point:
    def __init__(self, coordinates: List[float], name: str):
        self.coordinates = coordinates
        self.name = name

    def __lt__(self, other):
        # to reverse PriorityQueue's default ordering criteria
        return self.distance(utils.query_point) > other.distance(utils.query_point)

    def passes_filter(self):
        if utils.mode != "no filtering":
            return len(self.name) % 2 == 0
        else:
            return True

    def __str__(self):
        return self.name

    def distance(self, other):
        temp = 0
        for i in range(len(self.coordinates)):
            temp += (self.coordinates[i] - other.coordinates[i]) ** 2
        return math.sqrt(temp)
