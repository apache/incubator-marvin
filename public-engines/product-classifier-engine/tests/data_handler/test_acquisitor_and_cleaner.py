#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_product_classifier_engine.data_handler import AcquisitorAndCleaner
import pandas as pd


@mock.patch('marvin_product_classifier_engine.data_handler.acquisitor_and_cleaner.pd.read_csv')
@mock.patch('marvin_product_classifier_engine.data_handler.acquisitor_and_cleaner.MarvinData.download_file')
def test_execute(download_file_mocked, read_csv_mocked, mocked_params):

    read_csv_mocked.return_value = pd.DataFrame(data={'nome': ['grand', 'harry'], 'descricao': ['GTA5', 'potter'], 'categoria': ['game', 'livro']})

    ac = AcquisitorAndCleaner()
    ac.execute(params=mocked_params)

    download_file_mocked.assert_called_with("https://s3.amazonaws.com/automl-example/produtos.csv")
    read_csv_mocked.assert_called_once()
    assert str(ac.marvin_initial_dataset['categoria'][0]) == 'game'
    assert not ac._params
