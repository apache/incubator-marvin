#!/usr/bin/env python
# coding=utf-8

"""Trainer engine action.

Use this module to add the project main code.
"""

import sklearn_crfsuite

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseTraining

__all__ = ['Trainer']


logger = get_logger('trainer')


class Trainer(EngineBaseTraining):

    def __init__(self, params, **kwargs):
        super(Trainer, self).__init__(**kwargs)

    def execute(self, **kwargs):
        crf = sklearn_crfsuite.CRF(
                                    algorithm='lbfgs', 
                                    c1=0.10789964607864502, 
                                    c2=0.082422264927260847, 
                                    max_iterations=100, 
                                    all_possible_transitions=True
                                )
        crf.fit(self.marvin_dataset['train'][0], self.marvin_dataset['train'][1])
        self.marvin_model = crf

        logger.info("Model trained to recognize the following entities: ")
        labels = list(self.marvin_model.classes_)
        labels.remove('O')  # O is used to tag no entity
        logger.info(labels)
