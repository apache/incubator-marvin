#!/usr/bin/env python
# coding=utf-8

"""AcquisitorAndCleaner engine action.

Use this module to add the project main code.
"""
import nltk
import os

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseDataHandler

__all__ = ['AcquisitorAndCleaner']


logger = get_logger('acquisitor_and_cleaner')


class AcquisitorAndCleaner(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(AcquisitorAndCleaner, self).__init__(**kwargs)

    def execute(self, **kwargs):
        nltk.download(info_or_id='conll2002', download_dir=os.environ["MARVIN_DATA_PATH"])
        train_sents = list(nltk.corpus.conll2002.iob_sents('esp.train'))
        test_sents = list(nltk.corpus.conll2002.iob_sents('esp.testb'))
        self.initial_dataset = {"train_sents": train_sents,
                                "test_sents": test_sents}

        logger.info("An example of training sentences:")
        for annotated_token in self.initial_dataset["train_sents"][2]:
            logger.info(annotated_token)
