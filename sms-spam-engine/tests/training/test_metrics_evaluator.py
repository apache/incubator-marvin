#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_sms_spam_engine.training import MetricsEvaluator
from sklearn.naive_bayes import MultinomialNB
import pandas as pd


@mock.patch('marvin_sms_spam_engine.training.metrics_evaluator.accuracy_score')
def test_execute(accuracy_score_mocked, mocked_params):

    feature_df = pd.DataFrame(data={'col1': [1, 2], 'col2': [3, 4]})
    label_df = pd.DataFrame(data={'col1': [0, 1]})

    clf_tmp = MultinomialNB().fit(feature_df, label_df)
    model_mocked = {"clf": clf_tmp}

    data_source = {
        "X_test": [1, 2],
        "y_test": [3, 4]
    }

    ac = MetricsEvaluator(model=model_mocked, dataset=data_source)
    ac.execute(params=mocked_params)

    accuracy_score_mocked.assert_called_once()
    assert not ac._params
