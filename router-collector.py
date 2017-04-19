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

class MetricCollector(object):
    def __init__(self, name, description, labels, mtype="GAUGE"):
        self.name = name
        self.description = description
        self.labels = labels
        self.mtype = mtype

    def metric(self, label_values, value):
        m = None
        if self.mtype == "GAUGE":
            m = GaugeMetricFamily(self.name, self.description, labels=self.labels)
        elif self.mtype == "COUNTER":
            m = CounterMetricFamily(self.name, self.description, labels=self.labels)
        else:
            raise("Unknown type " + self.mtype)
        m.add_metric(label_values, value)
        return m

class RouterCollector(object):
    def __init__(self):
        self.collectormap = {'org.apache.qpid.dispatch.router': [
                MetricCollector('connectionCount', 'Number of connections to router', ['routerId']),
                MetricCollector('linkCount', 'Number of links to router', ['routerId']),
                MetricCollector('addrCount', 'Number of addresses defined in router', ['routerId']),
                MetricCollector('autoLinkCount', 'Number of auto links defined in router', ['routerId']),
                MetricCollector('linkRouteCount', 'Number of link routers defined in router', ['routerId'])
            ],
            'org.apache.qpid.dispatch.router.link': [
                MetricCollector('unsettledCount', 'Number of unsettled messages', ['name', 'connectionId']),
                MetricCollector('deliveryCount', 'Number of delivered messages', ['name', 'connectionId'], "COUNTER"),
                MetricCollector('releasedCount', 'Number of released messages', ['name', 'connectionId'], "COUNTER"),
                MetricCollector('rejectedCount', 'Number of rejected messages', ['name', 'connectionId'], "COUNTER"),
                MetricCollector('acceptedCount', 'Number of accepted messages', ['name', 'connectionId'], "COUNTER"),
                MetricCollector('undeliveredCount', 'Number of undelivered messages', ['name', 'connectionId']),
                MetricCollector('capacity', 'Capacity of link', ['name', 'connectionId'])
            ]}

    def collect(self):
        for entity in self.collectormap:
            response = self.collect_metric(entity)
            if response != None:
                attributes = response.body["attributeNames"]
                for result in response.body["results"]:
                    result_map = {}
                    for i in range(0, len(attributes)):
                        result_map[attributes[i]] = result[i]

                    for collector in self.collectormap[entity]:
                        labels = []
                        for l in collector.labels:
                            labels.append(result_map[l])
                        yield collector.metric(labels, int(result_map[collector.name]))

    def collect_metric(self, entityType):
        try:
            client = SyncRequestResponse(BlockingConnection("127.0.0.1:5672", 30), "$management")
            try:
                properties = {}
                properties["entityType"] = entityType
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
