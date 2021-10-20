#!/usr/bin/env python
# coding=utf-8

# Copyright [2020] [Apache Software Foundation]
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import os
import sys
import csv
import json
import time
import multiprocessing
from json.decoder import JSONDecodeError
import matplotlib.pyplot as plt
from .docker import get_stats
from .log import get_logger
from .misc import generate_timestamp

logger = get_logger("benchmark")

#to-do
def create_or_get_benchmark_folder():
    _folder_path = os.path.join(os.environ['MARVIN_DATA_PATH'], 'benchmarks')
    if not os.path.exists(_folder_path):
        os.makedirs(_folder_path)
    return _folder_path

def create_or_return_poi(timestamp):
    _folder_path = create_or_get_benchmark_folder()
    _file_path = os.path.join(_folder_path, 'poi_{0}.json'.format(timestamp))
    if not os.path.exists(_file_path):
        with open(_file_path, 'w') as _:
            pass
    return _file_path

def get_internal_keys(input_data, *args, default=None):
    _data = input_data
    for arg in args:
        try:
            _data = _data[arg]
        except (KeyError, ValueError, TypeError, AttributeError):
            logger.error("Failed getting {0} key...".format(arg))
            return default
    return _data

def calculate_disk_io(data_input):
    _stats = get_internal_keys(data_input, "blkio_stats", "io_service_bytes_recursive")
    if not _stats:
        return 0, 0
    r = 0
    w = 0
    for stat in _stats:
        if stat["op"] == "Read":
            r += stat["value"]
        elif stat["op"] == "Write":
            w += stat["value"]
    r = r / 1000 if (r > 0) else 0
    w = w / 1000 if (w > 0) else 0
    return r, w

def calculate_network_bytes(data_input):
    networks = get_internal_keys(data_input, "networks")
    if not networks:
        return 0, 0
    r = 0
    t = 0
    for if_name, data in networks.items():
        logger.debug("getting stats for interface %r", if_name)
        r += data["rx_bytes"]
        t += data["tx_bytes"]
    r = r / 1000 if (r > 0) else 0
    t = t / 1000 if (t > 0) else 0
    return r, t

def calculate_cpu_percent(input_data):
    cpu_count = len(get_internal_keys(input_data, "cpu_stats", "cpu_usage", "percpu_usage"))
    cpu_percent = 0.0
    cpu_delta = float(get_internal_keys(input_data, "cpu_stats", "cpu_usage", "total_usage")) - \
                float(get_internal_keys(input_data, "precpu_stats", "cpu_usage", "total_usage"))
    system_delta = float(get_internal_keys(input_data, "cpu_stats", "system_cpu_usage")) - \
                   float(get_internal_keys(input_data, "precpu_stats", "system_cpu_usage"))
    if system_delta > 0.0:
        cpu_percent = cpu_delta / system_delta * 100.0 * cpu_count
    return cpu_percent

def filter_data(input_data, initial_time):
    _data = input_data
    _r_net, _t_net = calculate_network_bytes(_data)
    _r_disk, _w_disk = calculate_disk_io(_data)
    _time = time.time() - initial_time
    return (_time,
            calculate_cpu_percent(_data),
            int(float(_data["memory_stats"]["usage"]) * (10 >> 6)),
            int(_r_disk),
            int(_w_disk),
            int(_r_net),
            int(_t_net))

def get_and_persist_stats(engine_name, initial_time, timestamp):
    _stats = get_stats(engine_name)
    _colleted_stats = filter_data(_stats, initial_time)
    _filename = 'benchmark_{0}.csv'.format(timestamp)
    _path = os.path.join(create_or_get_benchmark_folder(), _filename)
    with open(_path, 'a') as f:
        writer = csv.writer(f)
        writer.writerow(_colleted_stats)

def repeat_stats_call(engine_name, timestamp, initial_time):
    while True:
        getattr(sys.modules[__name__],
                'get_and_persist_stats')(engine_name, initial_time, timestamp)

def benchmark_thread(engine_name, timestamp, initial_time=time.time()):
    return multiprocessing.Process(target=repeat_stats_call, 
                                    args=(engine_name, timestamp, initial_time,))

def create_poi(key, value, timestamp):
    _file_path = create_or_return_poi(timestamp)
    pois = None
    with open(_file_path, 'r') as f:
        try:
            pois = json.load(f)
        except JSONDecodeError:
            pois = {}
    pois[key] = value
    with open(_file_path, 'w') as f:
        json.dump(pois, f)

def read_poi(timestamp):
    _file_path = create_or_return_poi(timestamp)
    pois = None
    with open(_file_path, 'r') as f:
        try:
            pois = json.load(f)
        except:
            pois = {}

    return pois

def read_csv(timestamp):
    _file_name = 'benchmark_{0}.csv'.format(timestamp)
    _file_path = os.path.join(create_or_get_benchmark_folder(), _file_name)
    print(_file_path)
    _data_dict = {
        'time': [],
        'cpu': [],
        'memory': [],
        'r_disk': [],
        'w_disk': [],
        'r_net': [],
        't_net': []
    }
    if os.path.exists(_file_path):
        with open(_file_path, 'r') as f:
            try:
                csv_buffer = csv.reader(f)
                for row in csv_buffer:
                    _data_dict['time'].append(float(row[0]))
                    _data_dict['cpu'].append(float(row[1]))
                    _data_dict['memory'].append(float(row[2]))
                    _data_dict['r_disk'].append(float(row[3]))
                    _data_dict['w_disk'].append(float(row[4]))
                    _data_dict['r_net'].append(float(row[5]))
                    _data_dict['t_net'].append(float(row[6]))
            except:
                logger.error('Something went wrong when writing csv file.')
    return _data_dict

def make_graph(name, label, timestamp):
    info_dict = read_csv(timestamp)
    time_dict = read_poi(timestamp)

    plt.plot(info_dict['time'], info_dict[name], color = "r")

    for key, value in time_dict.items():
        x_line_annotation = value
        x_text_annotation = value   
        plt.axvline(x=x_line_annotation, linestyle='dashed', alpha=0.5, color='black')
        t = plt.text(x=x_text_annotation, y=max(info_dict[name])/2, s=key, alpha=0.5, color='black')
        t.set_bbox(dict(facecolor='white', alpha=0.5, edgecolor='white'))

    plt.xlabel('Time (s)')
    plt.ylabel(label)

    plt.show()