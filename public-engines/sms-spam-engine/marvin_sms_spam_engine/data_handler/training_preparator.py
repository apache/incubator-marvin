#!/usr/bin/env python
# coding=utf-8

"""TrainingPreparator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from marvin_python_toolbox.engine_base import EngineBaseDataHandler
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.model_selection import train_test_split

__all__ = ['TrainingPreparator']


logger = get_logger('training_preparator')


class TrainingPreparator(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(TrainingPreparator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        X_train, X_test, y_train, y_test = train_test_split(
            self.marvin_initial_dataset["text"], self.marvin_initial_dataset["label"],
            test_size=params["test_size"], random_state=params["random_state"])

        vect = CountVectorizer()
        vect.fit(X_train)

        self.marvin_dataset = {
            "X_train": vect.transform(X_train),
            "X_test": vect.transform(X_test),
            "y_train": y_train,
            "y_test": y_test,
            "vect": vect
        }

