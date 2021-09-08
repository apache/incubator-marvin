#!/usr/bin/env python
# coding=utf-8

"""Predictor engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBasePrediction

__all__ = ['Predictor']


logger = get_logger('predictor')


class Predictor(EngineBasePrediction):

    def __init__(self, **kwargs):
        super(Predictor, self).__init__(**kwargs)

    def execute(self, input_message, params, **kwargs):
        from sklearn import datasets
        iris = datasets.load_iris()
        prediction = self.marvin_model.predict(input_message)
        final_prediction = iris.target_names[prediction][0]

        return final_prediction
