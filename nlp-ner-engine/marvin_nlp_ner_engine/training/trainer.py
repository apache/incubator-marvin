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

    def __init__(self, **kwargs):
        super(Trainer, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        crf = sklearn_crfsuite.CRF(
            algorithm='lbfgs',
            c1=0.10789964607864502,
            c2=0.082422264927260847,
            max_iterations=100,
            all_possible_transitions=True
        )
        crf.fit(self.marvin_dataset['X_train'], self.marvin_dataset['y_train'])

        self.marvin_model = {
            'crf': crf
        }

