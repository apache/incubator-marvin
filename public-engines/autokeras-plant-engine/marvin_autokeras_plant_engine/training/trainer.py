#!/usr/bin/env python
# coding=utf-8

"""Trainer engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseTraining

__all__ = ['Trainer']


logger = get_logger('trainer')


class Trainer(EngineBaseTraining):

    def __init__(self, **kwargs):
        super(Trainer, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        import autokeras as ak
        # Initialize the ImageClassifier.
        clf = ak.ImageClassifier(max_trials=1)
        # Search for the best model.
        clf.fit(self.marvin_dataset['train_X'], self.marvin_dataset['train_y'], epochs=1)

        self.marvin_model = clf

