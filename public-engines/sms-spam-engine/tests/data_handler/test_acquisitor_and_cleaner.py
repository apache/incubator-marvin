#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

import pandas as pd
from marvin_sms_spam_engine.data_handler import AcquisitorAndCleaner


@mock.patch('marvin_sms_spam_engine.data_handler.acquisitor_and_cleaner.pd.read_csv')
@mock.patch('marvin_sms_spam_engine.data_handler.acquisitor_and_cleaner.MarvinData.download_file')
def test_execute(download_file_mocked, read_csv_mocked, mocked_params):

    read_csv_mocked.return_value = pd.DataFrame(data={'v1': ['ham', 'spam'], 'v2': [3, 4], 'Unnamed: 2': [1, 1], 'Unnamed: 3': [1, 1], 'Unnamed: 4': [1, 1]})

    ac = AcquisitorAndCleaner()
    ac.execute(params=mocked_params)

    download_file_mocked.assert_called_with("https://s3.amazonaws.com/marvin-engines-data/spam.csv")
    read_csv_mocked.assert_called_once()
    assert str(ac.marvin_initial_dataset['label_num'][0]) == '0'
    assert not ac._params
