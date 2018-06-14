#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_iris_species_engine.data_handler import AcquisitorAndCleaner


@mock.patch('marvin_iris_species_engine.data_handler.acquisitor_and_cleaner.pd.read_csv')
@mock.patch('marvin_python_toolbox.common.data.MarvinData.download_file')
def test_execute(download_mocked, csv_mocked, mocked_params):
    ac = AcquisitorAndCleaner()
    mocked_params["data_url"] = "www.test_url.com"
    ac.execute(params=mocked_params)

    download_mocked.assert_called_once_with(url='www.test_url.com')
    csv_mocked.assert_called_once()
