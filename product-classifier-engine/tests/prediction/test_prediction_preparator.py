#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_product_classifier_engine.prediction import PredictionPreparator
from sklearn.feature_extraction.text import CountVectorizer
import pandas as pd


def test_execute(mocked_params):

    test_df = pd.DataFrame(data={'col1': ["0", "1"]})
    vect = CountVectorizer()
    vect.fit(test_df)
    model_mocked = {"vect": vect}

    ac = PredictionPreparator(model=model_mocked)
    ac.execute(input_message=["this is mocked message"], params=mocked_params)

    assert not ac._params
