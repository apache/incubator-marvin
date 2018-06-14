#!/usr/bin/env python
# coding=utf-8

"""MetricsEvaluator engine action.

Use this module to add the project main code.
"""
from sklearn import metrics as sk_metrics

from .._compatibility import six
from .._logging import get_logger
from six import iteritems

from marvin_python_toolbox.engine_base import EngineBaseTraining

__all__ = ['MetricsEvaluator']


logger = get_logger('metrics_evaluator')


class MetricsEvaluator(EngineBaseTraining):

    def __init__(self, **kwargs):
        super(MetricsEvaluator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        _metrics = {}

        for m in self.marvin_model.keys():
            dataset_key = m.split("_")[-1]

            _test_X = self.marvin_dataset[dataset_key]['test_X']
            _test_y = self.marvin_dataset[dataset_key]['test_y']

            self.marvin_model[m].predict(_test_X)
            prediction = self.marvin_model[m].predict(_test_X)
            _metrics[m] = sk_metrics.accuracy_score(prediction, _test_y)

        _metrics = sorted(iteritems(_metrics), key=lambda kv: (kv[1], kv[0]), reverse=True)

        self.marvin_metrics = {
            "best_model": _metrics[0],
            "all_metrics": _metrics
        }
