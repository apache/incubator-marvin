#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_product_classifier_engine.data_handler import TrainingPreparator


class TestTrainingPreparator:
    def test_execute(self, mocked_params):
        test_dataset = {
            "text": ["GTA", "harry"],
            "categoria": ["game", "livro"]
        }

        mocked_params = {"test_size": 0.5, "random_state": 10}

        ac = TrainingPreparator(initial_dataset=test_dataset)
        ac.execute(params=mocked_params)

        assert str(ac.marvin_dataset["X_train"]) == '  (0, 1)\t1'
        assert str(ac.marvin_dataset["X_test"]) == '  (0, 0)\t1'
        assert ac.marvin_dataset["y_train"] == ["livro"]
        assert ac.marvin_dataset["y_test"] == ["game"]
        assert not ac._params
