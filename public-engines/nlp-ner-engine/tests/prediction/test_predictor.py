#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock
from sklearn_crfsuite import CRF
from marvin_nlp_ner_engine.prediction import Predictor


class TestPredictor:
    def test_execute(mocked_params):

        feature_mocked = ('O', 'feature_mocked')
        label_mocked = ('O', 'label___mocked')

        crf_mocked = CRF(
            algorithm='lbfgs',
            c1=0.10789964607864502,
            c2=0.082422264927260847,
            max_iterations=100,
            all_possible_transitions=True).fit(feature_mocked, label_mocked)

        model_mocked = {"crf": crf_mocked}

        ac = Predictor(model=model_mocked)
        ac.execute(input_message=['1', '2'], params=mocked_params)
