# global variables
maxheap = None

knn_threshold = None

query_point = None

t1 = t2 = t3 = t4 = 0

# input file source
input_path = "C:\\Users\\Sutp\\Desktop\\glove.6B.50d.txt"

# number of results wanted
k_results = 10

# options: "no filtering", "pre-query filtering", "post-query filtering", "concurrent filtering"
mode = "no filtering"

# estimated population proportion of points that can fulfill the filtering criteria
filter_coefficient = 0.5