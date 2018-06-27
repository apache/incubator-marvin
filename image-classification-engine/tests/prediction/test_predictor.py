#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_image_classification_engine.prediction import Predictor


def test_execute(mocked_params):

    mocked_model = mock.MagicMock()

    ac = Predictor(model=mocked_model)
    ac.execute(input_message="test_message", params=mocked_params)

    mocked_model.predict.assert_called_once_with('test_message')
