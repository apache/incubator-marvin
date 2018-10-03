#!/usr/bin/env python
# coding=utf-8

"""Trainer engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from marvin_python_toolbox.engine_base import EngineBaseTraining
from sklearn.naive_bayes import MultinomialNB

__all__ = ['Trainer']


logger = get_logger('trainer')


class Trainer(EngineBaseTraining):

    def __init__(self, **kwargs):
        super(Trainer, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        clf = MultinomialNB()
        clf.fit(self.marvin_dataset['X_train'], self.marvin_dataset['y_train'])

        self.marvin_model = {
            "clf": clf,
            "vect": self.marvin_dataset["vect"]
        }

