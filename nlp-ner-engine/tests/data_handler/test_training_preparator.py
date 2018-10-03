#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_nlp_ner_engine.data_handler import TrainingPreparator


class TestTrainingPreparator:
    def test_execute(self, mocked_params):
        test_dataset = {
            "train_sents": [[(u'Melbourne', u'NP', u'B-LOC'), 
                (u'(', u'Fpa', u'O'), (u'Australia', u'NP', u'B-LOC'), 
                (u')', u'Fpt', u'O'), (u',', u'Fc', u'O'), 
                (u'25', u'Z', u'O'), (u'may', u'NC', u'O'), 
                (u'(', u'Fpa', u'O'), (u'EFE', u'NC', u'B-ORG'), 
                (u')', u'Fpt', u'O'), (u'.', u'Fp', u'O')], 
                [(u'-', u'Fg', u'O')]],
            "test_sents": [[(u'Melbourne', u'NP', u'B-LOC'), 
                (u'(', u'Fpa', u'O'), (u'Australia', u'NP', u'B-LOC'), 
                (u')', u'Fpt', u'O'), (u',', u'Fc', u'O'), 
                (u'25', u'Z', u'O'), (u'may', u'NC', u'O'), 
                (u'(', u'Fpa', u'O'), (u'EFE', u'NC', u'B-ORG'), 
                (u')', u'Fpt', u'O'), (u'.', u'Fp', u'O')], 
                [(u'-', u'Fg', u'O')]]
        }

        ac = TrainingPreparator(initial_dataset=test_dataset)
        ac.execute(params=mocked_params)
