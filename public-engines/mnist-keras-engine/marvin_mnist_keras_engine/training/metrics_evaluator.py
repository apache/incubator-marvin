#!/usr/bin/env python
# coding=utf-8

"""MetricsEvaluator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from ..model_serializer import ModelSerializer
from marvin_python_toolbox.engine_base import EngineBaseTraining
import keras

__all__ = ['MetricsEvaluator']


logger = get_logger('metrics_evaluator')


class MetricsEvaluator(ModelSerializer, EngineBaseTraining):

    def __init__(self, **kwargs):
        super(MetricsEvaluator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        score = self.marvin_model.evaluate(self.marvin_dataset["X_train"], self.marvin_dataset["y_train"], verbose=1)
        print("Accuracy is: {} ".format(score[1]))

        self.marvin_metrics = {"Accuracy": score}

