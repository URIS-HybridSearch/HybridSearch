# Image Hybrid Search Engine based on VP-Tree

## Installation
1. Download the Flickr8k image dataset on [link](https://www.kaggle.com/datasets/adityajn105/flickr8k).

2. Clone the repository and place all images under a new folder HybridSearch\static\img.

3. Install requirements using pip install -r requirements.txt.

4. Run python server.py, and visit localhost:5000 with your browser.

## Explanations
Flickr8k is a dataset of 8091 images with English captions. Due to its mixed nature, it is very suitable for studying Hybrid Search.

Hybrid Search is a query that involves both structured (scalor) and unstructured (vector) data, aka a similarity search with attribute filtering.

In this demo, users can conduct a content-based image similarity search with 2 optional filters, text and size, to find images similar to an input with a particular size and/or whose captions contain some given text.

In addition to 2 existing algorithms (pre-query filtering and post-query filtering), we also implement our proposed method (concurrent filtering) to examine its performance. Details can be found in the technical report.

## Observations
It is obvious that concurrent filtering outperforms post-query filtering in all cases in terms of time, memory and the accuracy of the number of results.

While pre-query filtering is more efficient than concurrent filtering in a single query, its index is less general than that of concurrent filtering. Concurrent filtering uses a full index that is the same as similarity search without attribute filtering, regardless of the filtering criteria. However, pre-query filtering requires an index for every distinct filter. Usually, it is acceptable to use some additional memory to store a few indices to speed up the queries. However, there is a tradeoff and in some cases listing all possible filters may not be practical.

For example, in the demo, the image size filter splits the whole dataset into only 5 categories. If pre-query filtering is applied on this attribute, only 6 indices need to be built but the performance can be significantly improved. However, for the caption text filter, it is impossible to build an index for each filter because the user has infinitely many choices. Therefore, concurrent filtering is more suitable for this attribute. Another solution would be building pre-query filtering indices for some most common inputs, and leaving the others to concurrent filtering.

In conclusion, both pre-query filtering and concurrent filtering have advantages and disadvantages. The best choice depends on the specific application scenario.

## Notes
Indices are built on demand when a query is submitted. It may take up to several minutes to build an index.

Once the index is built, the typical query time is 0-20s depending on conditions.
