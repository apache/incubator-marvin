#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_nlp_ner_engine.training import MetricsEvaluator
from sklearn_crfsuite import CRF


@mock.patch('marvin_nlp_ner_engine.training.metrics_evaluator.metrics.flat_f1_score')
@mock.patch('marvin_nlp_ner_engine.training.metrics_evaluator.metrics.flat_classification_report')
def test_execute(report_mocked, score_mocked, mocked_params):

    data_source = {
        "X_test": ['1', '2'],
        "y_test": ['3', '4']
    }

    feature_mocked = ('O', 'feature_mocked')
    label_mocked = ('O', 'label___mocked')

    crf_mocked = CRF(
        algorithm='lbfgs',
        c1=0.10789964607864502,
        c2=0.082422264927260847,
        max_iterations=100,
        all_possible_transitions=True).fit(feature_mocked, label_mocked)

    model_mocked = {"crf": crf_mocked}

    ac = MetricsEvaluator(model=model_mocked, dataset=data_source)
    ac.execute(params=mocked_params)

    report_mocked.assert_called_once_with(['3', '4'], [['O'], ['O']], digits=3, labels=['_', 'a', 'b', 'c', 'd', 'e', 'k', 'l', 'm', 'o'])
    score_mocked.assert_called_once_with(['3', '4'], [['O'], ['O']], average='weighted', labels=['l', 'a', 'b', 'e', '_', 'm', 'o', 'c', 'k', 'd'])
