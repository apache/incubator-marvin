#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_sms_spam_engine.prediction import Predictor
from sklearn.naive_bayes import MultinomialNB
import pandas as pd


class TestPredictor:
    def test_execute(self, mocked_params):

        feature_df = pd.DataFrame(data={'col1': [1, 2], 'col2': [3, 4]})
        label_df = pd.DataFrame(data={'col1': [0, 1]})

        clf_tmp = MultinomialNB().fit(feature_df, label_df)
        model_mocked = {"clf": clf_tmp}

        ac = Predictor(model=model_mocked)
        ac.execute(input_message=[1, 2], params=mocked_params)
        assert not ac._params
