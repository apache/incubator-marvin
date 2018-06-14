#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

import pandas as pd

from marvin_iris_species_engine.data_handler import TrainingPreparator


class TestTrainingPreparator:
    def test_execute(self, mocked_params):
        mocked_params['test_size'] = 0.3
        mocked_params['random_state'] = 10
        data = {
            'SepalLengthCm': [1, 2],
            'SepalWidthCm': [3, 4],
            'PetalLengthCm': [5, 6],
            'PetalWidthCm': [7, 8],
            'Species': 'specie1'
            }
        test_dataset = pd.DataFrame(data=data)

        ac = TrainingPreparator(initial_dataset=test_dataset)
        ac.execute(params=mocked_params)
