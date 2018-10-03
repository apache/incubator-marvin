#!/usr/bin/env python
# coding=utf-8

"""Trainer engine action.

Use this module to add the project main code.
"""

from .._logging import get_logger
from keras.models import Sequential
from keras.layers import Dense, Dropout, Activation, Flatten
from keras.layers import Convolution2D, MaxPooling2D
import keras
from marvin_python_toolbox.engine_base import EngineBaseTraining
from ..model_serializer import ModelSerializer

__all__ = ['Trainer']


logger = get_logger('trainer')


class Trainer(ModelSerializer, EngineBaseTraining):

    def __init__(self, **kwargs):
        super(Trainer, self).__init__(**kwargs)

    def execute(self, params, **kwargs):

        keras.backend.clear_session()

        model = Sequential()
        model.add(Convolution2D(32, kernel_size=(3, 3), activation='relu', input_shape=(1, 28, 28), data_format="channels_first"))
        model.add(Convolution2D(32, 3, 3, activation='relu', input_shape=(1, 28, 28)))
        model.add(Convolution2D(32, 3, 3, activation='relu'))
        model.add(MaxPooling2D(pool_size=(2, 2)))
        model.add(Dropout(0.25))

        model.add(Flatten())
        model.add(Dense(128, activation='relu'))
        model.add(Dropout(0.5))
        model.add(Dense(10, activation='softmax'))

        model.compile(loss='categorical_crossentropy', optimizer='adam', metrics=['accuracy'])
        model.fit(self.marvin_dataset["X_train"], self.marvin_dataset["y_train"], batch_size=32, epochs=1, verbose=1)

        self.marvin_model = model

