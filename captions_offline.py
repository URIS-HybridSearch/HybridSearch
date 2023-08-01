import json

# Create an empty dictionary to store the captions
caption_dict = {}

# Open the captions.txt file and read its lines
with open('captions.txt', 'r') as f:
    for line in f.readlines():
        # Split each line into an image filename and a caption
        image_filename, caption = line.strip().split(',', 1)

        # If the image filename is already a key in the dictionary, add the caption to its list of captions
        if image_filename in caption_dict:
            caption_dict[image_filename].append(caption)
        # Otherwise, create a new key-value pair with the image filename and a list containing the caption
        else:
            caption_dict[image_filename] = [caption]

# Save the dictionary to a JSON file
with open('captions.json', 'w') as f:
    json.dump(caption_dict, f)