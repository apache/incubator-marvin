#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_nlp_ner_engine.training import MetricsEvaluator


class TestMetricsEvaluator:
    def test_execute(self, mocked_params):
        ac = MetricsEvaluator(params=mocked_params)
        ac.execute()
        assert ac.params == mocked_params