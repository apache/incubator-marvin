#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from keras.models import Sequential
from marvin_mnist_keras_engine.training import MetricsEvaluator


@mock.patch('marvin_mnist_keras_engine.training.metrics_evaluator.print')
def test_execute(mocked_print, mocked_params):

    mocked_model = mock.MagicMock()

    test_dataset = {
        "X_train": "train_data",
        "X_test": "test_data",
        "y_train": "train_data",
        "y_test": "test_data"
    }

    ac = MetricsEvaluator(model=mocked_model, dataset=test_dataset)
    ac.execute(params=mocked_params)

    mocked_model.evaluate.assert_called_once()
