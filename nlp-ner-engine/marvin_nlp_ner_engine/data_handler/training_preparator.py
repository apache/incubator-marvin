#!/usr/bin/env python
# coding=utf-8

"""TrainingPreparator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseDataHandler

__all__ = ['TrainingPreparator']


logger = get_logger('training_preparator')


class TrainingPreparator(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(TrainingPreparator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
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


        def sent2labels(sent):
            return [label for token, postag, label in sent]


        X_train = [sent2features(s) for s in self.marvin_initial_dataset['train_sents']]
        y_train = [sent2labels(s) for s in self.marvin_initial_dataset['train_sents']]

        X_test = [sent2features(s) for s in self.marvin_initial_dataset['test_sents']]
        y_test = [sent2labels(s) for s in self.marvin_initial_dataset['test_sents']]

        self.marvin_dataset = {
            'X_train': X_train,
            'y_train': y_train,
            'X_test': X_test,
            'y_test': y_test
        }

