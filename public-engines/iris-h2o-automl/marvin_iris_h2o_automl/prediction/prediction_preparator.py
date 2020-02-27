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
        import h2o
        import pandas as pd

        input_message = {'SepalLengthCm': [input_message[0]], 'SepalWidthCm': [input_message[1]],
                         'PetalLengthCm': [input_message[2]], 'PetalWidthCm': [input_message[3]]}
        input_message = pd.DataFrame(data=input_message)
        input_message = h2o.H2OFrame.from_python(input_message)

        return input_message
