#!/usr/bin/env python
# coding=utf-8

"""TrainingPreparator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from marvin_python_toolbox.engine_base import EngineBaseDataHandler

__all__ = ['TrainingPreparator']


logger = get_logger('training_preparator')


class TrainingPreparator(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(TrainingPreparator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        from sklearn.model_selection import train_test_split
        from sklearn import preprocessing
        import numpy as np

        train_dict, test_dict = self.marvin_initial_dataset
        X_dataset = np.array(train_dict['image'])
        y_dataset = np.array(train_dict['class'])

        X_train, X_test, y_train, y_test = train_test_split(X_dataset, y_dataset, test_size=0.33, random_state=42)

        le = preprocessing.LabelEncoder()
        le.fit(y_train)
        y_train_encoded = le.transform(y_train)
        y_test_encoded = le.transform(y_test)
        print(y_train_encoded)

        self.marvin_dataset = {'train_X': X_train, 'train_y': y_train_encoded, 'test_X': X_test, 'test_y': y_test_encoded}

