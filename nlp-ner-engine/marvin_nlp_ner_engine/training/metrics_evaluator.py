#!/usr/bin/env python
# coding=utf-8

"""MetricsEvaluator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger

from sklearn_crfsuite import metrics as skmetrics

from marvin_python_toolbox.engine_base import EngineBaseTraining

__all__ = ['MetricsEvaluator']


logger = get_logger('metrics_evaluator')


class MetricsEvaluator(EngineBaseTraining):

    def __init__(self, params, **kwargs):
        super(MetricsEvaluator, self).__init__(**kwargs)

    def execute(self, **kwargs):
        _metrics = {}
        labels = list(self.marvin_model.classes_)
        labels.remove('O')
        y_pred = self.marvin_model.predict(self.marvin_dataset['test'][0])
        _metrics["weighted_f1"] = skmetrics.flat_f1_score(self.marvin_dataset['test'][1], y_pred, average='weighted', labels=labels)
        logger.info(_metrics['weighted_f1'])
        
        sorted_labels = sorted(labels, key=lambda name: (name[1:], name[0]))
        _metrics['report'] = skmetrics.flat_classification_report(self.marvin_dataset['test'][1], y_pred, labels=sorted_labels, digits=3)
        logger.info(_metrics['report'])
        self.marvin_metrics = _metrics