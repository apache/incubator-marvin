#!/usr/bin/env python
# coding=utf-8

"""PredictionPreparator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBasePrediction

__all__ = ['PredictionPreparator']


logger = get_logger('prediction_preparator')


class PredictionPreparator(EngineBasePrediction):

    def __init__(self, **kwargs):
        super(PredictionPreparator, self).__init__(**kwargs)

    def execute(self, input_message, params, **kwargs):
        # Given the input: input_message = {"age": 50, "class": 3, "sex": 0}
        # Transform the message into a correctly ordered list for the model

        key_order = {"Age": 0, "Pclass": 1, "Sex": 2, "Fare": 3}
        input_message = [input_message[i] for i in sorted(input_message, key=key_order.__getitem__)]

        return input_message
