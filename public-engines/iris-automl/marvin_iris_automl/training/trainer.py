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
        import autosklearn.classification

        automl = autosklearn.classification.AutoSklearnClassifier(
            time_left_for_this_task=120,
            per_run_time_limit=30,
            resampling_strategy='cv',
            resampling_strategy_arguments={'folds': 5},
        )

        automl.fit(self.marvin_dataset['train_X'].copy(), self.marvin_dataset['train_y'].copy())
        automl.refit(self.marvin_dataset['train_X'].copy(), self.marvin_dataset['train_y'].copy())

        # Using fitted_pipeline_ method to get model
        self.marvin_model = automl

