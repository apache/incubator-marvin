#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_titanic_engine.prediction import PredictionPreparator


def test_execute(mocked_params):

    message = {"Age": 50, "Pclass": 3, "Sex": 0}

    ac = PredictionPreparator()
    ac.execute(input_message=message, params=mocked_params)

    assert not ac._params
