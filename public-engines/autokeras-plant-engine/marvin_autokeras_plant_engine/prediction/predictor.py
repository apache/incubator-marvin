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
        from marvin_python_toolbox.common.image_loader import ImageLoader
        CLASS = dict(zip(ImageLoader.get_class_names("plant"), [x for x in range(0, 12)]))
        INV_CLASS = {v: k for k, v in CLASS.items()}

        final_prediction = INV_CLASS[self.marvin_model.predict(input_message).argmax()]

        return final_prediction
