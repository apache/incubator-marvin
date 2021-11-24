import json
import time
import requests
import sys
from .log import get_logger

logger = get_logger('utils.api')

class ApiError(Exception):
    pass

def run_action(host, action_name):
    resp = requests.post("{0}/{1}".format(host, action_name), json={}, verify=False)
    if resp.status_code != 200:
        raise ApiError('POST /{0}/ {1}'.format(action_name, resp.status_code))
    return resp.json()

def wait_action(host, action_name, protocol):
    retries = 3
    while True:
        payload = {
            'protocol': protocol
        }
        resp = requests.get("{0}/{1}/status".format(host, action_name),
                             verify=False, params=payload)
        if resp.status_code != 200:
            retries -= 1
            if retries == 0:
                raise ApiError('PUSH /{0}/status {1}'.format(action_name, resp.status_code))
            continue
        retries = 3
        resp_dict = json.loads(resp.json()['result'])
        if resp_dict['status']['name'] == 'finished':
            break
        else:
            time.sleep(1)

def run_acquisitor(host, wait=False):
    try:
        result = run_action(host, 'acquisitor')
        if wait:
            wait_action(host, 'acquisitor', result['result'])
    except ApiError as e:
        logger.error(e)
        sys.exit(1)

def run_tpreparator(host, wait=False):
    try:
        result = run_action(host, 'tpreparator')
        if wait:
            wait_action(host, 'tpreparator', result['result'])
    except ApiError as e:
        logger.error(e)
        sys.exit(1)

def run_trainer(host, wait=False):
    try:
        result = run_action(host, 'trainer')
        if wait:
            wait_action(host, 'trainer', result['result'])
    except ApiError:
        logger.error(e)
        sys.exit(1)

def run_evaluator(host, wait=False):
    try:
        result = run_action(host, 'evaluator')
        if wait:
            wait_action(host, 'evaluator', result['result'])
    except ApiError:
        logger.error(e)
        sys.exit(1)