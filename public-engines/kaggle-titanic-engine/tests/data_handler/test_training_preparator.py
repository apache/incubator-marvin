#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

import pandas as pd
from marvin_titanic_engine.data_handler import TrainingPreparator


@mock.patch('marvin_titanic_engine.data_handler.training_preparator.StratifiedShuffleSplit')
@mock.patch('marvin_titanic_engine.data_handler.training_preparator.len')
def test_execute(len_mocked, split_mocked, mocked_params):

    test_dataset = {
        "train": pd.DataFrame({'Sex': [1, 2, 3], 'B': [4, 5, 6], 'C': [7, 8, 9]}),
        "test": pd.DataFrame({'Sex': [1, 2, 3], 'B': [4, 5, 6], 'C': [7, 8, 9]})
    }

    mocked_params = {
        "pred_cols": ["Sex", "B"],
        "dep_var": "C"
    }

    ac = TrainingPreparator(initial_dataset=test_dataset)
    ac.execute(params=mocked_params)

    len_mocked.assert_called()
    split_mocked.assert_called_with(n_splits=5, random_state=0, test_size=0.6)
