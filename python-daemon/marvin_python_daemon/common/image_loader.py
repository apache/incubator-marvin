#!/usr/bin/env python
# coding=utf-8

# Copyright [2020] [Apache Software Foundation]
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Image Data Load Module from Autokeras.
"""

import os
import os
from tqdm import tqdm
import imageio
import numpy as np
from PIL import Image
import urllib.request
from skimage.transform import resize as imresize
from .utils import check_path
from .exceptions import InvalidConfigException
from .._logging import get_logger
from .data import MarvinData


logger = get_logger('common.image_loader')

class ImageLoader(MarvinData):
    _key = 'MARVIN_DATA_PATH'

    @classmethod
    def get_class_names(cls,relpath):
        """
        Get class names from the following sources in order of priority:
        1.  Filesystem
        :param relpath: path relative to "data_path"
        :return: list - array with class names
        """

        path = os.path.join(cls.get_data_path(), relpath)
        path = os.path.join(path, "train")
        names = [name for name in os.listdir(path) if os.path.isdir(os.path.join(path, name))]
        names.sort()
        return names

    @classmethod
    def img_reshape(cls,img,shape):
        img = imresize(img, (shape[0], shape[1], 3))
        return img

    
    @classmethod
    def img_class(cls,path):
        return str(path.split('/')[-2])

    @classmethod
    def fill_dict(cls,paths, data_dict,shape):
        text = ''
        if 'train' in paths[0]:
            text = 'Start fill train_dict'
        elif 'test' in paths[0]:
            text = 'Start fill test_dict'

        for p in tqdm(paths, ascii=True, ncols=85, desc=text):
            img = imageio.imread(p)
            img = cls.img_reshape(img,shape)
            data_dict['image'].append(img)
            if 'train' in paths[0]:
                data_dict['class'].append(cls.img_class(p))

        return data_dict

    @classmethod
    def fill_df(cls,paths, train_dict,shape):
        train_dict['image'] = []
        text = 'Start fill train_dict'
        for p in tqdm(paths, ascii=True, ncols=85, desc=text):
            img = imageio.imread(p)
            img = cls.img_reshape(img,shape)
            train_dict['image'].append(img)

        return train_dict


    @classmethod
    def img_to_predict(cls,link,shape):
        """
        Load image from the following sources in order of priority:
        1. Web link
        :param link: link relative to image
        :param shape: shape of image
        :return: img - array of image
        """
        img = Image.open(urllib.request.urlopen(link))
        img = np.asarray(img)
        img = cls.img_reshape(img,shape)
        return(img)

    @classmethod
    def reader_from_directory(cls,relpath,shape):
        """
        Load data from the following sources in order of priority:
        1. Filesystem
        :param relpath: path relative to "data_path"
        :param shape: shape of images
        :return: tuple of dict - data content
        """
        path = os.path.join(cls.data_path, relpath)
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
            'class': []
        }
        test_dict = {
            'image': []
        }

        train_dict = cls.fill_dict(train_path, train_dict,shape)
        test_dict = cls.fill_dict(test_path, test_dict,shape)
        return train_dict, test_dict

    @classmethod
    def reader_from_dataframe(cls,dataframe,directory,shape):
        """
        Load data from the following sources in order of priority:
        1. Filesystem
        :param dataframe:  Pandas dataframe containing the filepaths relative to 
        directory (or absolute paths if directory is None) of the images in a string 
        column. it must include the y_col column with the class/es of each image.
        :param directory: string, path to the directory to read images from. 
        If None, data in x_col column should be absolute paths.
        :param shape: shape of images
        :return: dict - data content
        """
        path = os.path.join(cls.get_data_path(), directory)

        columns = dataframe.columns

        dataframe = dataframe.apply(lambda x: path+os.sep+x if x.name == columns[0] else x)

        dataframe = {
            'image': dataframe[columns[0]],
            'class': dataframe[columns[1]]
        }

        dataframe = cls.fill_df(dataframe.get("image"), dataframe,shape)
        
        return dataframe


