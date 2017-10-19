#!/usr/bin/env python
# coding=utf-8

"""PredictionPreparator engine action.

Use this module to add the project main code.
"""
import cv2
import numpy as np
from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBasePrediction

__all__ = ['PredictionPreparator']


logger = get_logger('prediction_preparator')


class PredictionPreparator(EngineBasePrediction):

    def __init__(self, **kwargs):
        super(PredictionPreparator, self).__init__(**kwargs)

    def execute(self, input_message, **kwargs):
        image = cv2.imread(input_message["message"])
        image = cv2.resize(image, (150, 150))
        image = image[np.newaxis, :, :, (2, 1, 0)]
        return image