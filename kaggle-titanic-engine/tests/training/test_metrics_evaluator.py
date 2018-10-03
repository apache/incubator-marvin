#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

import pandas as pd
from marvin_titanic_engine.training import MetricsEvaluator


@mock.patch('marvin_titanic_engine.training.metrics_evaluator.metrics.confusion_matrix')
@mock.patch('marvin_titanic_engine.training.metrics_evaluator.metrics.classification_report')
def test_execute(report_mocked, matrix_mocked, mocked_params):

    test_dataset = {
        "X_test": pd.DataFrame({'Sex': [1, 2, 3], 'B': [4, 5, 6], 'C': [7, 8, 9]}),
        "y_test": pd.DataFrame({'Sex': [1, 2, 3], 'B': [4, 5, 6], 'C': [7, 8, 9]}),
        "X_train": pd.DataFrame({'Sex': [1, 2, 3], 'B': [4, 5, 6], 'C': [7, 8, 9]}),
        "y_train": pd.DataFrame({'Sex': [1, 2, 3], 'B': [4, 5, 6], 'C': [7, 8, 9]}),
        "sss": mock.MagicMock()
    }

    mocked_params = {
        "pred_cols": ["Sex", "B"],
        "dep_var": "C"
    }

    model_mocked = {
        # "model_type": "test_type",
        "test": mock.MagicMock(),
        "rf": mock.MagicMock()
    }

    ac = MetricsEvaluator(model=model_mocked, dataset=test_dataset)
    ac.execute(params=mocked_params)

    report_mocked.assert_called()
    matrix_mocked.assert_called()
    model_mocked["test"].predict.assert_called_once()
