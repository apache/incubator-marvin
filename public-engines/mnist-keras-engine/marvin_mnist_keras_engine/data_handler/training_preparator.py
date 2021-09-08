#!/usr/bin/env python
# coding=utf-8

"""TrainingPreparator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from keras.utils import np_utils
from marvin_python_toolbox.engine_base import EngineBaseDataHandler

__all__ = ['TrainingPreparator']


logger = get_logger('training_preparator')


class TrainingPreparator(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(TrainingPreparator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):

        X_train = self.marvin_initial_dataset["X_train"]
        X_train = X_train.reshape(X_train.shape[0], 1, 28, 28)
        X_train = X_train.astype('float32')
        X_train /= 255

        X_test = self.marvin_initial_dataset["X_test"]
        X_test = X_test.reshape(X_test.shape[0], 1, 28, 28)
        X_test = X_test.astype('float32')
        X_test /= 255

        nb_classes = 10

        y_train = np_utils.to_categorical(self.marvin_initial_dataset["y_train"], nb_classes)
        y_test = np_utils.to_categorical(self.marvin_initial_dataset["y_test"], nb_classes)

        self.marvin_dataset = {
            "X_train": X_train,
            "y_train": y_train,
            "X_test": X_test,
            "y_test": y_test
        }

