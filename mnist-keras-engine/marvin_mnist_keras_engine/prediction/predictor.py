#!/usr/bin/env python
# coding=utf-8

"""Predictor engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from ..model_serializer import ModelSerializer
from marvin_python_toolbox.engine_base import EngineBasePrediction

__all__ = ['Predictor']


logger = get_logger('predictor')


class Predictor(ModelSerializer, EngineBasePrediction):

    def __init__(self, **kwargs):
        super(Predictor, self).__init__(**kwargs)

    def execute(self, input_message, params, **kwargs):
        predicted = self.marvin_model.predict_classes(input_message)
        acc = self.marvin_model.predict(input_message)[0][predicted[0]]
        print("The image has the number {} with {} accuracy".format(predicted, acc))

        final_prediction = predicted[0]

        return final_prediction
