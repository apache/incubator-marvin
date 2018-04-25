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
        y_pred = self.marvin_model.predict(input_message)
        
        sentence = []
        entities = {}
        
        for i, token in enumerate(input_message[0]):
            word = token["word.lower()"]
            sentence.append(word)
            
            label = y_pred[0][i]
            if label != "O":
                if label in entities:
                    entities[label].append(word)
                else:
                    entities[label] = [word]
        example_of_prediction = {}
        example_of_prediction["sentence"] = ' '.join(sentence)
        example_of_prediction["entities_found"] = {}
        for k, v in entities.items():
            example_of_prediction["entities_found"][k] = ' '.join(v)

        return example_of_prediction