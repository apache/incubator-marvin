#!/usr/bin/env python
# coding=utf-8

"""PredictionPreparator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from ..data_handler.training_preparator import TrainingPreparator

from marvin_python_toolbox.engine_base import EngineBasePrediction

__all__ = ['PredictionPreparator']


logger = get_logger('prediction_preparator')


class PredictionPreparator(EngineBasePrediction):

    def __init__(self, **kwargs):
        super(PredictionPreparator, self).__init__(**kwargs)

    def execute(self, input_message, params, **kwargs):
        def word2features(sent, i):
            word = sent[i][0]
            postag = sent[i][1]

            features = {
                'bias': 1.0,
                'word.lower()': word.lower(),
                'word[-3:]': word[-3:],
                'word[-2:]': word[-2:],
                'word.isupper()': word.isupper(),
                'word.istitle()': word.istitle(),
                'word.isdigit()': word.isdigit(),
                'postag': postag,
                'postag[:2]': postag[:2],
            }
            if i > 0:
                word1 = sent[i - 1][0]
                postag1 = sent[i - 1][1]
                features.update({
                    '-1:word.lower()': word1.lower(),
                    '-1:word.istitle()': word1.istitle(),
                    '-1:word.isupper()': word1.isupper(),
                    '-1:postag': postag1,
                    '-1:postag[:2]': postag1[:2],
                })
            else:
                features['BOS'] = True

            if i < len(sent) - 1:
                word1 = sent[i + 1][0]
                postag1 = sent[i + 1][1]
                features.update({
                    '+1:word.lower()': word1.lower(),
                    '+1:word.istitle()': word1.istitle(),
                    '+1:word.isupper()': word1.isupper(),
                    '+1:postag': postag1,
                    '+1:postag[:2]': postag1[:2],
                })
            else:
                features['EOS'] = True

            return features


        def sent2features(sent):
            return [word2features(sent, i) for i in range(len(sent))]


        input_message = sent2features(input_message)
        print(input_message[0])

        return input_message
