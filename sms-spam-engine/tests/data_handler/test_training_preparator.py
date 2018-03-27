#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_sms_spam_engine.data_handler import TrainingPreparator


class TestTrainingPreparator:
    def test_execute(self, mocked_params):
        #The test_dataset is our data source, containing sms messages and their respective labels.
        test_dataset = {
            "text": ["foo", "bar", "foo", "bar", "foo", "bar"],
            "label": ["ham", "spam", "ham", "spam", "ham", "spam"]
        }

        mocked_params = {"test_size": 0.3, "random_state": 10}

        ac = TrainingPreparator(initial_dataset=test_dataset)
        ac.execute(params=mocked_params)

        assert str(ac.marvin_dataset["X_train"]) == '  (0, 1)\t1\n  (1, 0)\t1\n  (2, 1)\t1\n  (3, 0)\t1'
        assert str(ac.marvin_dataset["X_test"]) == '  (0, 1)\t1\n  (1, 0)\t1'
        assert ac.marvin_dataset["y_train"] == ["ham", "spam", "ham", "spam"]
        assert ac.marvin_dataset["y_test"] == ["ham", "spam"]
        assert not ac._params
