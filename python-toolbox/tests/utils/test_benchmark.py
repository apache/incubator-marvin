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

try:
    import mock
except ImportError:
    import unittest.mock as mock

from marvin_python_toolbox.utils.benchmark import create_or_get_benchmark_folder
from marvin_python_toolbox.utils.benchmark import calculate_cpu_percent, calculate_disk_io, calculate_network_bytes
from marvin_python_toolbox.utils.benchmark import create_or_return_poi, create_poi
from marvin_python_toolbox.utils.benchmark import make_graph, read_csv, read_poi, filter_data

mocked_data = {
    "memory_stats": {
        "usage": 0
    },
    "cpu_stats": {
        "cpu_usage": {
            "total_usage": 0,
            "percpu_usage": []
        },
        "system_cpu_usage": 0,
    },
    "precpu_stats": {
        "cpu_usage": {
            "total_usage": 0,
            "percpu_usage": []
        },
        "system_cpu_usage": 0
    },
    "networks": {
        "interface": {
            "rx_bytes": 0,
            "tx_bytes": 0
        }
    },
    "blkio_stats": {
        "io_service_bytes_recursive": {
            "stat": {
                "op": {
                    "Read": {
                        "value": 0
                    },
                    "Write": {
                        "value": 0
                    }
                }
            }
        }
    }
}

def test_create_or_get_benchmark_folder():
    path = create_or_get_benchmark_folder()
    assert path == os.path.join(os.environ['MARVIN_DATA_PATH'], 'benchmarks')

def test_create_or_return_poi():
    timestamp = 'mocked_timestamp'
    path = create_or_return_poi(timestamp)
    assert path == os.path.join(create_or_get_benchmark_folder(), 
                                'poi_{0}.json'.format(timestamp))

@mock.patch("marvin_python_toolbox.utils.benchmark.os.path.exists")
@mock.patch("marvin_python_toolbox.utils.benchmark.open")
def test_read_csv(open_mocked, exists_mocked):
    path = os.path.join(create_or_get_benchmark_folder(), 'benchmark_mocked.csv')
    read_csv('mocked')
    exists_mocked.assert_called_with(path)
    open_mocked.assert_called_with(path, 'r')

@mock.patch("marvin_python_toolbox.utils.benchmark.open")
def test_read_poi(open_mocked):
    timestamp = 'mocked'
    path = create_or_return_poi(timestamp)
    read_poi('mocked')
    open_mocked.assert_called_with(path, 'r')

@mock.patch("marvin_python_toolbox.utils.benchmark.read_csv")
@mock.patch("marvin_python_toolbox.utils.benchmark.read_poi")
@mock.patch("marvin_python_toolbox.utils.benchmark.plt.show")
def test_make_graph(show_mocked, poi_mocked, csv_mocked):
    timestamp = 'mocked'
    make_graph('mocked_name', 'mocked_label', timestamp)
    poi_mocked.assert_called_with(timestamp)
    csv_mocked.assert_called_with(timestamp)
    show_mocked.assert_called_once()

@mock.patch("marvin_python_toolbox.utils.benchmark.open")
@mock.patch("marvin_python_toolbox.utils.benchmark.json.load")
@mock.patch("marvin_python_toolbox.utils.benchmark.json.dump")
def test_create_poi(dump_mocked, load_mocked, open_mocked):
    timestamp = 'mocked'
    create_poi('mock', 'ed', timestamp)
    load_mocked.assert_called_once()
    dump_mocked.assert_called_once()
    open_mocked.assert_called()


@mock.patch("marvin_python_toolbox.utils.benchmark.calculate_network_bytes")
@mock.patch("marvin_python_toolbox.utils.benchmark.calculate_disk_io")
def test_filter_data(disk_io_mocked, net_bytes_mocked):
    disk_io_mocked.return_value = (0,0)
    net_bytes_mocked.return_value = (0,0)
    filter_data(mocked_data, 0)
    disk_io_mocked.assert_called_once()
    net_bytes_mocked.assert_called_once()

@mock.patch("marvin_python_toolbox.utils.benchmark.get_internal_keys")
def test_calculate_disk_io(int_keys_mocked):
    calculate_disk_io(mocked_data)
    int_keys_mocked.assert_called_with(mocked_data, "blkio_stats", "io_service_bytes_recursive")

@mock.patch("marvin_python_toolbox.utils.benchmark.get_internal_keys")
def test_calculate_network_bytes(int_keys_mocked):
    calculate_network_bytes(mocked_data)
    int_keys_mocked.assert_called_with(mocked_data, "networks")

@mock.patch("marvin_python_toolbox.utils.benchmark.get_internal_keys")
def test_calculate_cpu_percent(int_keys_mocked):
    calculate_cpu_percent(mocked_data)
    int_keys_mocked.assert_called()
