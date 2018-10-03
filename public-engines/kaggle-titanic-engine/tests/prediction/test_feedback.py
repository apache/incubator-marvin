#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_titanic_engine.prediction import Feedback


class TestFeedback:
    def test_execute(self, mocked_params):
        fb = Feedback()
        fb.execute(input_message="fake message", params=mocked_params)
        assert not fb._params