#!/usr/bin/env python
# coding=utf-8

"""Predictor engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBasePrediction
from ..model_serializer import ModelSerializer

__all__ = ['Predictor']


logger = get_logger('predictor')


class Predictor(ModelSerializer, EngineBasePrediction):

    def __init__(self, **kwargs):
        super(Predictor, self).__init__(**kwargs)

    def execute(self, input_message, params, **kwargs):
        final_prediction = self.marvin_model.predict(input_message).as_data_frame().values[0][0]

        return final_prediction
