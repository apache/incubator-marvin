#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_mnist_keras_engine.training import Trainer


@mock.patch('marvin_mnist_keras_engine.training.trainer.Sequential.fit')
def test_execute(fit_mocked, mocked_params):

    test_dataset = {
        "X_train": "train_data",
        "X_test": "test_data",
        "y_train": "train_data",
        "y_test": "test_data"
    }

    ac = Trainer(dataset=test_dataset)
    ac.execute(params=mocked_params)

    fit_mocked.assert_called_once_with('train_data', 'train_data', batch_size=32, epochs=1, verbose=1)
