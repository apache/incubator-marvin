#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

import pandas as pd
from sklearn.model_selection import StratifiedShuffleSplit
from marvin_titanic_engine.training import Trainer


@mock.patch('marvin_titanic_engine.training.trainer.round')
@mock.patch('marvin_titanic_engine.training.trainer.GridSearchCV')
def test_execute(grid_mocked, round_mocked, mocked_params):

    test_dataset = {
        "X_train": pd.DataFrame({'Sex': [1, 2, 3], 'B': [4, 5, 6], 'C': [7, 8, 9]}),
        "y_train": pd.DataFrame({'Sex': [1, 2, 3], 'B': [4, 5, 6], 'C': [7, 8, 9]}),
        "sss": mock.MagicMock()
    }

    mocked_params = {
        "pred_cols": ["Sex", "B"],
        "dep_var": "C",
        "svm": [
            {"C": [1, 10, 100], "gamma": [0.01, 0.001], "kernel": ["linear"]},
            {"C": [1, 10, 100], "gamma": [0.01, 0.001], "kernel": ["rbf"]}
        ],
        "rf": {
            "max_depth": [3],
            "random_state": [0],
            "min_samples_split": [2],
            "min_samples_leaf": [1],
            "n_estimators": [20],
            "bootstrap": [True, False],
            "criterion": ["gini", "entropy"]
        }
    }

    ac = Trainer(dataset=test_dataset)
    ac.execute(params=mocked_params)

    grid_mocked.assert_called()
    round_mocked.assert_called()
