#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_image_classification_engine.data_handler import AcquisitorAndCleaner


@mock.patch('marvin_image_classification_engine.data_handler.acquisitor_and_cleaner.open')
def test_read_samples(mocked_open):

    ac = AcquisitorAndCleaner()
    ac.read_samples(filename="test_filename")

    mocked_open.assert_called_once_with('test_filename', 'r')


@mock.patch('marvin_image_classification_engine.data_handler.acquisitor_and_cleaner.open')
@mock.patch('marvin_image_classification_engine.data_handler.acquisitor_and_cleaner.MarvinData.download_file')
@mock.patch('marvin_image_classification_engine.data_handler.acquisitor_and_cleaner.os.path.join')
def test_execute(mocked_join, mocked_download, mocked_open, mocked_params):

    mocked_params = {
        'DATA': 'http://www.test_data.org',
        'TRAIN': 'http://www.train_data.org',
        'VALID': 'http://www.valid_data.org'
    }
    mocked_join.return_value = 'test_value'
    mocked_download.return_value = 'test_data'

    ac = AcquisitorAndCleaner()
    ac.execute(params=mocked_params)

    mocked_join.assert_called()
