#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_iris_species_engine.prediction import PredictionPreparator


def test_execute(mocked_params):
    ac = PredictionPreparator()
    ac.execute(input_message="fake message", params=mocked_params)