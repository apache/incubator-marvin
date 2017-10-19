#!/usr/bin/env python
# coding=utf-8

"""MetricsEvaluator engine action.

Use this module to add the project main code.
"""
import os
import numpy as np
import cv2
from sklearn import metrics as sk_metrics
from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseTraining

__all__ = ['MetricsEvaluator']


logger = get_logger('metrics_evaluator')


class MetricsEvaluator(EngineBaseTraining):

    def __init__(self, **kwargs):
        super(MetricsEvaluator, self).__init__(**kwargs)

    def generate_samples(self, image_path, fnames, w=150, h=150):
        indx = 0
        while True:
            for fname in fnames['positive']:
                image = cv2.imread(os.path.join(image_path, fname + '.jpg'))
                image = cv2.resize(image, (w, h))
                image = image[np.newaxis, :, :, (2, 1, 0)]
                label = np.array([1])
                yield (image, label)

                if indx >= len(fnames['negative']):
                    indx = 0

                fname = fnames['negative'][indx]
                image = cv2.imread(os.path.join(image_path, fname + '.jpg'))
                image = cv2.resize(image, (w, h))
                image = image[np.newaxis, :, :, (2, 1, 0)]
                label = np.array([0])
                yield (image, label)

                indx += 1

    def execute(self, **kwargs):
        validation_data = self.generate_samples(self.params['IMAGES'],
                                                self.dataset['val'])
        y_true = []
        y_pred = []
        for _ in range(len(self.dataset['val']['positive'])):
            image, label = validation_data.next()
            predicted = self.model.predict(image)
            y_true.append(label)
            y_pred.append(predicted[0])

        metrics = {}
        metrics['accuracy'] = sk_metrics.accuracy_score(y_true, y_pred)
        print(metrics)
        self.metrics = metrics
