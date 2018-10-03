#!/usr/bin/env python
# coding=utf-8

"""PredictionPreparator engine action.

Use this module to add the project main code.
"""
import os
import cv2
import numpy as np
from marvin_python_toolbox.common.data import MarvinData
from ..model_serializer import ModelSerializer
from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBasePrediction

__all__ = ['PredictionPreparator']


logger = get_logger('prediction_preparator')


class PredictionPreparator(ModelSerializer, EngineBasePrediction):

    def __init__(self, **kwargs):
        super(PredictionPreparator, self).__init__(**kwargs)

    def execute(self, input_message, params, **kwargs):
        image = cv2.imread(os.path.join(MarvinData.data_path, input_message["message"]))
        image = cv2.resize(image, (150, 150))
        image = image[np.newaxis, :, :, (2, 1, 0)]
        return image