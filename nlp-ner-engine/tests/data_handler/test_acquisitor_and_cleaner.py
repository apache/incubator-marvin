#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

import nltk
from mock import ANY
from marvin_nlp_ner_engine.data_handler import AcquisitorAndCleaner


@mock.patch('marvin_nlp_ner_engine.data_handler.acquisitor_and_cleaner.nltk.download')
@mock.patch('marvin_nlp_ner_engine.data_handler.acquisitor_and_cleaner.nltk.corpus.conll2002.iob_sents')
def test_execute(sents_mocked, download_mocked, mocked_params):
    ac = AcquisitorAndCleaner()
    ac.execute(params=mocked_params)

    download_mocked.assert_called_with('conll2002')
    sents_mocked.assert_called_with('esp.testb')
