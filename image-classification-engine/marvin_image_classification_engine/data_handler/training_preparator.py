#!/usr/bin/env python
# coding=utf-8

"""TrainingPreparator engine action.

Use this module to add the project main code.
"""
import os
import cv2
from marvin_python_toolbox.common.data import MarvinData
from marvin_python_toolbox.engine_base import EngineBaseDataHandler
from .._compatibility import six
from .._logging import get_logger


__all__ = ['TrainingPreparator']


logger = get_logger('training_preparator')


class TrainingPreparator(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(TrainingPreparator, self).__init__(**kwargs)
        self.image_path = os.path.join(MarvinData.data_path, self.params['TRAIN_IMAGES'])
        if not os.path.exists(self.image_path):
            os.makedirs(self.image_path)
            os.makedirs(os.path.join(self.image_path, '0'))
            os.makedirs(os.path.join(self.image_path, '1'))

    def convert_images(self, image_path, fnames, w=150, h=150):
        data = []
        logger.info("Resizing images.")

        for nn, (fname, label) in enumerate(fnames):
            if nn % 100 == 0:
                logger.info("{}/{}".format(nn, len(fnames)))

            label = 0 if int(label) == -1 else 1
            imname = os.path.join(self.image_path, str(label), fname + '.jpg')
            if not os.path.exists(imname):
                image = cv2.imread(os.path.join(MarvinData.data_path, image_path, fname + '.jpg'))
                image = cv2.resize(image, (w, h))
                cv2.imwrite(imname, image)
            data.append((imname, label))
        return data

    def execute(self, **kwargs):
        train, val = self.initial_dataset

        training_data = self.convert_images(self.params['IMAGES'],
                                            train,
                                            w=self.params['W'],
                                            h=self.params['H'])

        validation_data = self.convert_images(self.params['IMAGES'],
                                              val,
                                              w=self.params['W'],
                                              h=self.params['H'])

        self.dataset = {'train': training_data, 'val': validation_data}
