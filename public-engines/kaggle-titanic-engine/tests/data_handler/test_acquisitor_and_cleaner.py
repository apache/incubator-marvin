#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_titanic_engine.data_handler import AcquisitorAndCleaner


@mock.patch('marvin_python_toolbox.common.data.MarvinData.download_file')
@mock.patch('marvin_titanic_engine.data_handler.acquisitor_and_cleaner.pd.read_csv')
def test_execute(read_csv_mocked, download_mocked, mocked_params):

    ac = AcquisitorAndCleaner()
    ac.execute(params=mocked_params)

    download_mocked.assert_called_with('https://s3.amazonaws.com/marvin-engines-data/titanic/test.csv')
    read_csv_mocked.assert_called()
