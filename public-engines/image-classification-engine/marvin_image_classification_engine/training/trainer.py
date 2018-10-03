#!/usr/bin/env python
# coding=utf-8

"""Trainer engine action.

Use this module to add the project main code.
"""
import os
import numpy as np
import cv2
from keras.layers import Activation, Reshape, Dropout, Dense, Flatten
from keras.layers import AtrousConvolution2D, Conv2D, MaxPooling2D, Conv2DTranspose, UpSampling2D
from keras.models import Sequential
from keras import callbacks, optimizers
from ..model_serializer import ModelSerializer
from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseTraining

__all__ = ['Trainer']


logger = get_logger('trainer')


class Trainer(ModelSerializer, EngineBaseTraining):
    def __init__(self, **kwargs):
        super(Trainer, self).__init__(**kwargs)

    def build_model(self, w=150, h=150, trainable=True):
        model = Sequential()
        model.add(Conv2D(32, (3, 3), input_shape=(w, h, 3)))
        model.add(Activation('relu'))
        model.add(MaxPooling2D(pool_size=(2, 2)))

        model.add(Conv2D(32, (3, 3)))
        model.add(Activation('relu'))
        model.add(MaxPooling2D(pool_size=(2, 2)))

        model.add(Conv2D(64, (3, 3)))
        model.add(Activation('relu'))
        model.add(MaxPooling2D(pool_size=(2, 2)))

        if trainable:
            model.add(Flatten())
            model.add(Dense(64))
            model.add(Activation('relu'))
            model.add(Dropout(0.5))
            model.add(Dense(1))
            model.add(Activation('softmax'))

        print(model.summary())
        return model

    def generate_samples(self, fnames):
        while True:
            for fname, label in fnames:
                image = cv2.imread(fname)
                image = image[np.newaxis, :, :, (2, 1, 0)]
                yield (image, np.array([int(label)]))


    def execute(self, params, **kwargs):
        model = self.build_model(trainable=True)
        model.compile(loss='binary_crossentropy',
                      optimizer=optimizers.SGD(lr=params['LEARNING_RATE'], momentum=params['MOMENTUM']),
                      metrics=['accuracy'])

        training_data = self.generate_samples(self.marvin_dataset['train'])
        validation_data = self.generate_samples(self.marvin_dataset['val'])

        model.fit_generator(training_data,
                            steps_per_epoch=params['STEPS'],
                            epochs=params['EPOCHS'],
                            validation_data=validation_data,
                            validation_steps=params['VAL_STEPS'])

        self.marvin_model = model
