#!/usr/bin/env python
# coding=utf-8

"""TrainingPreparator engine action.

Use this module to add the project main code.
"""

from sklearn.cross_validation import train_test_split

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseDataHandler

__all__ = ['TrainingPreparator']


logger = get_logger('training_preparator')


class TrainingPreparator(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(TrainingPreparator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        # Joined dataset
        train, test = train_test_split(self.marvin_initial_dataset, test_size=params["test_size"])

        train_X = train[['SepalLengthCm', 'SepalWidthCm', 'PetalLengthCm', 'PetalWidthCm']]
        train_y = train.Species

        test_X = test[['SepalLengthCm', 'SepalWidthCm', 'PetalLengthCm', 'PetalWidthCm']]
        test_y = test.Species

        # Separeted dataset
        petal = self.marvin_initial_dataset[['PetalLengthCm', 'PetalWidthCm', 'Species']]
        sepal = self.marvin_initial_dataset[['SepalLengthCm', 'SepalWidthCm', 'Species']]

        train_p, test_p = train_test_split(petal, test_size=params["test_size"], random_state=params["random_state"])
        train_x_p = train_p[['PetalWidthCm', 'PetalLengthCm']]
        train_y_p = train_p.Species
        test_x_p = test_p[['PetalWidthCm', 'PetalLengthCm']]
        test_y_p = test_p.Species

        train_s, test_s = train_test_split(sepal, test_size=params["test_size"], random_state=params["random_state"])
        train_x_s = train_s[['SepalWidthCm', 'SepalLengthCm']]
        train_y_s = train_s.Species
        test_x_s = test_s[['SepalWidthCm', 'SepalLengthCm']]
        test_y_s = test_s.Species

        self.marvin_dataset = {
            'petals': {'train_X': train_x_p, 'train_y': train_y_p, 'test_X': test_x_p, 'test_y': test_y_p},
            'sepals': {'train_X': train_x_s, 'train_y': train_y_s, 'test_X': test_x_s, 'test_y': test_y_s},
            'joined': {'train_X': train_X, 'train_y': train_y, 'test_X': test_X, 'test_y': test_y}
        }
