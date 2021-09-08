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
        import sklearn.model_selection
        X, y = self.marvin_initial_dataset.data, self.marvin_initial_dataset.target
        X_train, X_test, y_train, y_test = sklearn.model_selection.train_test_split(X, y, random_state=1)

        self.marvin_dataset = {'train_X': X_train, 'train_y': y_train, 'test_X': X_test, 'test_y': y_test}

