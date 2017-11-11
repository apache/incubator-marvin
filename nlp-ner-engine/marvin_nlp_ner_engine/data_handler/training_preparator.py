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

    @classmethod
    def word2features(cls, sent, i):
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
            word1 = sent[i-1][0]
            postag1 = sent[i-1][1]
            features.update({
                '-1:word.lower()': word1.lower(),
                '-1:word.istitle()': word1.istitle(),
                '-1:word.isupper()': word1.isupper(),
                '-1:postag': postag1,
                '-1:postag[:2]': postag1[:2],
            })
        else:
            features['BOS'] = True
            
        if i < len(sent)-1:
            word1 = sent[i+1][0]
            postag1 = sent[i+1][1]
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

    @classmethod
    def sent2features(cls, sent):
        return [cls.word2features(sent, i) for i in range(len(sent))]

    @classmethod
    def sent2labels(cls, sent):
        return [label for token, postag, label in sent]

    @classmethod
    def sent2tokens(cls, sent):
        return [token for token, postag, label in sent]

    def execute(self, **kwargs):
        X_train = [TrainingPreparator.sent2features(s) for s in self.initial_dataset['train_sents']]
        y_train = [TrainingPreparator.sent2labels(s) for s in self.initial_dataset['train_sents']]

        X_test = [TrainingPreparator.sent2features(s) for s in self.initial_dataset['test_sents']]
        y_test = [TrainingPreparator.sent2labels(s) for s in self.initial_dataset['test_sents']]

        self.dataset = {"train": [X_train, y_train],
                        "test": [X_test, y_test]}