#!/usr/bin/env python
# coding=utf-8

"""AcquisitorAndCleaner engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
import nltk
import os
from marvin_python_toolbox.engine_base import EngineBaseDataHandler

__all__ = ['AcquisitorAndCleaner']


logger = get_logger('acquisitor_and_cleaner')


class AcquisitorAndCleaner(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(AcquisitorAndCleaner, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        nltk.download('conll2002')
        train_sents = list(nltk.corpus.conll2002.iob_sents('esp.train'))
        test_sents = list(nltk.corpus.conll2002.iob_sents('esp.testb'))

        self.marvin_initial_dataset = {
            'train_sents': train_sents,
            'test_sents': test_sents
        }

