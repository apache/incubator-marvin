#!/usr/bin/env python
# coding=utf-8

"""MetricsEvaluator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from sklearn_crfsuite import metrics
from marvin_python_toolbox.engine_base import EngineBaseTraining

__all__ = ['MetricsEvaluator']


logger = get_logger('metrics_evaluator')


class MetricsEvaluator(EngineBaseTraining):

    def __init__(self, **kwargs):
        super(MetricsEvaluator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        labels = list(self.marvin_model['crf'].classes_)
        labels.remove('O')
        y_pred = self.marvin_model['crf'].predict(self.marvin_dataset['X_test'])

        score = metrics.flat_f1_score(self.marvin_dataset['y_test'], y_pred, average='weighted', labels=labels)

        sorted_labels = sorted(
            labels,
            key=lambda name: (name[1:], name[0])
        )
        report = metrics.flat_classification_report(
            self.marvin_dataset['y_test'], y_pred, labels=sorted_labels, digits=3
        )

        self.marvin_metrics = {
            'score': score,
            'report': report
        }

        print('Balanced F-score: ' + str(score))
        print('\nClassification Report: \n' + str(report))

