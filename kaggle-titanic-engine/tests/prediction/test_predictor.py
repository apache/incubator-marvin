#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_titanic_engine.prediction import Predictor


class TestPredictor:
    def test_execute(self, mocked_params):

        model_mocked = {
            "rf": mock.MagicMock(),
            "svm": mock.MagicMock()
        }

        ac = Predictor(model=model_mocked)
        ac.execute(input_message="fake message", params=mocked_params)

        model_mocked["rf"].predict.assert_called_once()
        model_mocked["svm"].predict.assert_called_once()
