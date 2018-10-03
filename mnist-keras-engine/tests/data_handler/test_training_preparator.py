#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_mnist_keras_engine.data_handler import TrainingPreparator


def test_execute(mocked_params):

    test_dataset = {
        "X_train": mock.MagicMock(),
        "X_test": mock.MagicMock(),
        "y_train": mock.MagicMock(),
        "y_test": mock.MagicMock()
    }

    ac = TrainingPreparator(initial_dataset=test_dataset)
    ac.execute(params=mocked_params)

    test_dataset["X_train"].reshape.assert_called_once()
    test_dataset["X_test"].reshape.assert_called_once()
