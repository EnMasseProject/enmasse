#!/usr/bin/env python
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

"""
A simple client for retrieving router metrics and exposing them

"""
from __future__ import print_function, unicode_literals

import optparse
from proton import Message, Url, ConnectionException, Timeout
from proton.utils import SyncRequestResponse, BlockingConnection
from proton.handlers import IncomingMessageHandler
import sys
from prometheus_client import start_http_server, Gauge
import random
import time
from prometheus_client.core import GaugeMetricFamily, CounterMetricFamily, REGISTRY

class RouterCollector(object):
    def collect(self):
        response = self.get_metrics()
        if response != None:
            m = GaugeMetricFamily('num_connections', 'Number of connections to router', labels=['container', 'role', 'dir'])
            attributes = response.body["attributeNames"]
            for result in response.body["results"]:
                result_map = {}
                for i in range(0, len(attributes)):
                    result_map[attributes[i]] = result[i]
                m.add_metric([result_map["container"], result_map["role"], result_map["dir"]], 1.0)

            yield m

    def get_metrics(self):
        try:
            client = SyncRequestResponse(BlockingConnection("127.0.0.1:5672", 30), "$management")
            try:
                properties = {}
                properties["entityType"] = "org.apache.qpid.dispatch.connection"
                properties["operation"] = "QUERY"
                properties["name"] = "self"
                message = Message(body=None, properties=properties)
                response = client.call(message)
                return response
            finally:
                client.connection.close()
        except:
            e = sys.exc_info()[0]
            print("Error querying router for metrics: %s" % e)
            return None

if __name__ == '__main__':
    # Start up the server to expose the metrics.
    REGISTRY.register(RouterCollector())
    start_http_server(8080)
    while True:
        time.sleep(5)
