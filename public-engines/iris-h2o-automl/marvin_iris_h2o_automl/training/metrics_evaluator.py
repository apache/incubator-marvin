#!/usr/bin/env python
# coding=utf-8

"""MetricsEvaluator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseTraining
from ..model_serializer import ModelSerializer

__all__ = ['MetricsEvaluator']


logger = get_logger('metrics_evaluator')


class MetricsEvaluator(ModelSerializer, EngineBaseTraining):

    def __init__(self, **kwargs):
        super(MetricsEvaluator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        import h2o
        from sklearn import metrics

        # h2o.init()

        y_test = self.marvin_dataset['test_X']['Species']
        self.marvin_dataset['test_X'].drop(columns='Species', inplace=True)

        teste = h2o.H2OFrame.from_python(self.marvin_dataset['test_X'])
        preds = self.marvin_model.predict(teste).as_data_frame()['predict'].values
        self.marvin_metrics = metrics.accuracy_score(y_test, preds)

