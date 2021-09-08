#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_iris_h2o_automl.prediction import Predictor


class TestPredictor:
    def test_execute(self, mocked_params):
        ac = Predictor()
        ac.execute(input_message="fake message", params=mocked_params)
        assert not ac._params