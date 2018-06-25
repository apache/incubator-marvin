#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_mnist_keras_engine.prediction import PredictionPreparator


@mock.patch('marvin_mnist_keras_engine.prediction.prediction_preparator.cv2.resize')
@mock.patch('marvin_mnist_keras_engine.prediction.prediction_preparator.cv2.imdecode')
@mock.patch('marvin_mnist_keras_engine.prediction.prediction_preparator.urlopen')
def test_execute(mocked_urlopen, mocked_imdecode, mocked_resize, mocked_params):

    message = ["test_message"]

    ac = PredictionPreparator(model="test_model")
    ac.execute(input_message=message, params=mocked_params)

    mocked_urlopen.assert_called_once_with(["test_message"])
    mocked_imdecode.assert_called_once()
    mocked_resize.assert_called_once()
