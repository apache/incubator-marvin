#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_iris_h2o_automl.training import Trainer


class TestTrainer:
    def test_execute(self, mocked_params):
        ac = Trainer()
        ac.execute(params=mocked_params)
        assert not ac._params
