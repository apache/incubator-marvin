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
        from sklearn import model_selection

        X_train, X_test = train_test_split(self.marvin_initial_dataset, random_state=1, test_size=0.3)

        self.marvin_dataset = {'train_X': X_train,  'test_X': X_test}

