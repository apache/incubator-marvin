#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_nlp_ner_engine.training import Trainer


@mock.patch('marvin_nlp_ner_engine.training.trainer.sklearn_crfsuite.CRF.fit')
def test_execute(fit_mocked, mocked_params):

    data_source = {
        "X_train": ["train datas"],
        "X_test": ["test datas"],
        "y_train": ["train labels"],
        "y_test": ["test labels"]
    }

    ac = Trainer(dataset=data_source)
    ac.execute(params=mocked_params)

    fit_mocked.assert_called_once_with(['train datas'], ['train labels'])
    assert ac.marvin_dataset["y_test"] == ["test labels"]
