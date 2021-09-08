#!/usr/bin/env python
# coding=utf-8

"""TrainingPreparator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from sklearn.feature_extraction.text import CountVectorizer
from sklearn.model_selection import train_test_split
from marvin_python_toolbox.engine_base import EngineBaseDataHandler

__all__ = ['TrainingPreparator']


logger = get_logger('training_preparator')


class TrainingPreparator(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(TrainingPreparator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):

        X_train, X_test, y_train, y_test = train_test_split(
            self.marvin_initial_dataset["text"],
            self.marvin_initial_dataset["categoria"],
            test_size=0.2,
            random_state=10
        )

        vect = CountVectorizer()
        vect.fit(self.marvin_initial_dataset["text"])

        self.marvin_dataset = {
            "X_train": vect.transform(X_train),
            "X_test": vect.transform(X_test),
            "y_train": y_train,
            "y_test": y_test,
            "vect": vect
        }

