#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock
from marvin_mnist_keras_engine.data_handler import AcquisitorAndCleaner


@mock.patch('marvin_mnist_keras_engine.data_handler.acquisitor_and_cleaner.mnist.load_data')
def test_execute(data_mocked, mocked_params):

    data_mocked.return_value = ([1, 2], [3, 4])

    ac = AcquisitorAndCleaner()
    ac.execute(params=mocked_params)

    data_mocked.assert_called_once()
