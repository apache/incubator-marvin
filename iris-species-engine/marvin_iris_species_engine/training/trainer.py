#!/usr/bin/env python
# coding=utf-8

"""Trainer engine action.

Use this module to add the project main code.
"""

from sklearn import svm
from sklearn.linear_model import LogisticRegression
from sklearn.tree import DecisionTreeClassifier
from sklearn.neighbors import KNeighborsClassifier

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseTraining

__all__ = ['Trainer']


logger = get_logger('trainer')


class Trainer(EngineBaseTraining):

    def __init__(self, **kwargs):
        super(Trainer, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        algorithms = {
            'svm': svm.SVC,
            'lr': LogisticRegression,
            'dt': DecisionTreeClassifier,
            'knn': KNeighborsClassifier
        }

        _model = {}
        for name in algorithms.keys():
            algorithm = algorithms[name]
            _model[name + '_petals'] = algorithm().fit(self.marvin_dataset['petals']['train_X'], self.marvin_dataset['petals']['train_y'])
            _model[name + '_sepals'] = algorithm().fit(self.marvin_dataset['sepals']['train_X'], self.marvin_dataset['sepals']['train_y'])
            _model[name + '_joined'] = algorithm().fit(self.marvin_dataset['joined']['train_X'], self.marvin_dataset['joined']['train_y'])

        self.marvin_model = _model
