#!/usr/bin/env python
# coding=utf-8

"""MetricsEvaluator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from sklearn.metrics import accuracy_score
from marvin_python_toolbox.engine_base import EngineBaseTraining

__all__ = ['MetricsEvaluator']


logger = get_logger('metrics_evaluator')


class MetricsEvaluator(EngineBaseTraining):

    def __init__(self, **kwargs):
        super(MetricsEvaluator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):

        y_prediction = self.marvin_model["clf"].predict(self.marvin_dataset["X_test"])

        accuracy = accuracy_score(y_prediction, self.marvin_dataset["y_test"])

        print(accuracy)

        self.marvin_metrics = accuracy

