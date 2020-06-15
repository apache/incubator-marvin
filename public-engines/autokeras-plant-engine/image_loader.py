
import os
from tqdm import tqdm
import imageio
import numpy as np
from PIL import Image
import urllib.request
from skimage.transform import resize as imresize


root_path = os.environ['HOME'] + "/marvin/data/plant"

def get_class_names():
    global root_path
    path = root_path
    path += "/train"
    names = [name for name in os.listdir(path) if os.path.isdir(os.path.join(path, name))]
    names.sort()
    return names


# Resize all image to 51x51 
def img_reshape(img):
    img = imresize(img, (51, 51, 3))
    return img

# get image tag
def img_label(path):
    return str(str(path.split('/')[-1]))

# get plant class on image
def img_class(path):
    return str(path.split('/')[-2])

# fill train and test dict
def fill_dict(paths, some_dict):
    text = ''
    if 'train' in paths[0]:
        text = 'Start fill train_dict'
    elif 'test' in paths[0]:
        text = 'Start fill test_dict'

    for p in tqdm(paths, ascii=True, ncols=85, desc=text):
        img = imageio.imread(p)
        img = img_reshape(img)
        some_dict['image'].append(img)
        some_dict['label'].append(img_label(p))
        if 'train' in paths[0]:
            some_dict['class'].append(img_class(p))

    return some_dict

def img_to_predict(link):
    img = Image.open(urllib.request.urlopen(link))
    img = np.asarray(img)
    img = img_reshape(img)
    return(img)


# read image from dir. and fill train and test dict
def reader():
    global root_path
    path = root_path
    file_ext = []
    train_path = []
    test_path = []

    for root, dirs, files in os.walk(path):
        if dirs != []:
            print('Root:\n'+str(root))
            print('Dirs:\n'+str(dirs))
        else:
            for f in files:
                ext = os.path.splitext(str(f))[1][1:]

                if ext not in file_ext:
                    file_ext.append(ext)

                if 'train' in root:
                    path = os.path.join(root, f)
                    train_path.append(path)
                elif 'test' in root:
                    path = os.path.join(root, f)
                    test_path.append(path)
    train_dict = {
        'image': [],
        'label': [],
        'class': []
    }
    test_dict = {
        'image': [],
        'label': []
    }

    train_dict = fill_dict(train_path, train_dict)
    test_dict = fill_dict(test_path, test_dict)
    return train_dict, test_dict
