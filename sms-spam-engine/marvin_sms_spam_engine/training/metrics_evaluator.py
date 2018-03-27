#!/usr/bin/env python
# coding=utf-8

"""MetricsEvaluator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from marvin_python_toolbox.engine_base import EngineBaseTraining
from sklearn.metrics import accuracy_score

__all__ = ['MetricsEvaluator']


logger = get_logger('metrics_evaluator')


class MetricsEvaluator(EngineBaseTraining):

    def __init__(self, **kwargs):
        super(MetricsEvaluator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        prediction = self.marvin_model["clf"].predict(self.marvin_dataset["X_test"])
        metrics = accuracy_score(prediction, self.marvin_dataset["y_test"])

        self.marvin_metrics = metrics

        print("Prediction accuracy: " + str(metrics))

