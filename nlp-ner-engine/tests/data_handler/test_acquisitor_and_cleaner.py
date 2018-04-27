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
def test_execute(download_mocked, mocked_params):
    ac = AcquisitorAndCleaner()
    ac.execute(params=mocked_params)

    download_mocked.assert_called_with(download_dir=ANY, info_or_id='conll2002')
