#!/usr/bin/env python
# coding=utf-8

"""MetricsEvaluator engine action.

Use this module to add the project main code.
"""
import os
import numpy as np
import cv2
from sklearn import metrics as sk_metrics
from keras.models import load_model
from .._compatibility import six
from .._logging import get_logger

from ..model_serializer import ModelSerializer
from marvin_python_toolbox.engine_base import EngineBaseTraining

__all__ = ['MetricsEvaluator']


logger = get_logger('metrics_evaluator')


class MetricsEvaluator(ModelSerializer, EngineBaseTraining):

    def __init__(self, **kwargs):
        super(MetricsEvaluator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        y_true = []
        y_pred = []
        for indx, (fname, label) in enumerate(self.marvin_dataset['val']):
            if indx == params["TEST_STEPS"]:
                break
            image = cv2.imread(fname)
            image = image[np.newaxis, :, :, (2, 1, 0)]
            predicted = self.marvin_model.predict(image)
            y_true.append(label)
            y_pred.append(predicted[0])

        metrics = {}
        metrics['accuracy'] = sk_metrics.accuracy_score(y_true, y_pred)
        logger.info(metrics)
        self.marvin_metrics = metrics

