#!/usr/bin/env python
# coding=utf-8

"""MetricsEvaluator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from marvin_python_toolbox.engine_base import EngineBaseTraining

__all__ = ['MetricsEvaluator']


logger = get_logger('metrics_evaluator')


class MetricsEvaluator(EngineBaseTraining):

    def __init__(self, **kwargs):
        super(MetricsEvaluator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        from sklearn.metrics import accuracy_score
        from marvin_python_toolbox.common.image_loader import ImageLoader

        CLASS = dict(zip(ImageLoader.get_class_names("plant"), [x for x in range(0, 12)]))
        INV_CLASS = {v: k for k, v in CLASS.items()}

        # Evaluate on the testing data.
        prob = self.marvin_model.predict(self.marvin_dataset['test_X'])

        self.marvin_metrics = accuracy_score(self.marvin_dataset['test_y'], [INV_CLASS[p.argmax()] for p in prob])

