#!/usr/bin/env python
# coding=utf-8

"""PredictionPreparator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from ..model_serializer import ModelSerializer
import numpy as np
import cv2
from six.moves.urllib.request import urlopen
from marvin_python_toolbox.engine_base import EngineBasePrediction

__all__ = ['PredictionPreparator']


logger = get_logger('prediction_preparator')


class PredictionPreparator(ModelSerializer, EngineBasePrediction):

    def __init__(self, **kwargs):
        super(PredictionPreparator, self).__init__(**kwargs)

    def execute(self, input_message, params, **kwargs):

        resp = urlopen(input_message)
        img = np.asarray(bytearray(resp.read()), dtype="uint8")

        img = cv2.imdecode(img, cv2.IMREAD_COLOR)
        img = cv2.resize(img, (28, 28))
        img = img[:, :, 0]

        input_message = img.reshape(1, 1, 28, 28)
        input_message = input_message.astype('float32')
        input_message /= 255

        return input_message
