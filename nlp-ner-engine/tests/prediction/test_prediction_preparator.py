#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_nlp_ner_engine.prediction import PredictionPreparator


@mock.patch('marvin_nlp_ner_engine.prediction.prediction_preparator.len')
@mock.patch('marvin_nlp_ner_engine.prediction.prediction_preparator.range')
def test_execute(range_mocked, len_mocked, mocked_params):

    message = [("train_token1", "train_postag1", "train_label1"), ("train_token2", "train_postag2", "train_label2")]
    len_mocked.return_value = 2
    range_mocked.return_value = [0, 1]

    ac = PredictionPreparator(model="test_model")
    ac.execute(input_message=message, params=mocked_params)

    len_mocked.assert_called_with([('train_token1', 'train_postag1', 'train_label1'), ('train_token2', 'train_postag2', 'train_label2')])
    range_mocked.assert_called_once()
