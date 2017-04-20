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
        self.label_list = []
        self.value_list = []
        if mtype == "GAUGE":
            self.metric_family = GaugeMetricFamily(self.name, self.description, labels=self.labels)
        elif mtype == "COUNTER":
            self.metric_family = CounterMetricFamily(self.name, self.description, labels=self.labels)
        else:
            raise("Unknown type " + mtype)

    def add(self, label_values, value):
        for idx, val in enumerate(self.label_list):
            if val == label_values:
                self.value_list[idx] += value
                return

        self.label_list.append(label_values)
        self.value_list.append(value)

    def metric(self):
        for idx, val in enumerate(self.label_list):
            self.metric_family.add_metric(val, self.value_list[idx])
        return self.metric_family

def to_map(response):
    retval = []
    if response == None:
        return retval

    attributes = response.body["attributeNames"]
    for result in response.body["results"]:
        m = {}
        for i in range(0, len(attributes)):
            m[attributes[i]] = result[i]
        retval.append(m)
    return retval



def clean_address(address):
    if address == None:
        return address
    elif address[0] == 'M':
        return address[2:]
    else:
        return address[1:]

def get_container_from_connections(connection_id, connections):
    for connection in connections:
        if connection_id == connection["identity"]:
            return connection["container"]
    return None

class RouterCollector(object):
    def get_collectormap(self):
        return {self.get_router: [
                MetricCollector('connectionCount', 'Number of connections to router', ['routerId']),
                MetricCollector('linkCount', 'Number of links to router', ['routerId']),

                MetricCollector('addrCount', 'Number of addresses defined in router', ['routerId']),
                MetricCollector('autoLinkCount', 'Number of auto links defined in router', ['routerId']),
                MetricCollector('linkRouteCount', 'Number of link routers defined in router', ['routerId'])
            ],
            self.get_links: [
                MetricCollector('linkCount', 'Number of links to router', ['address']),
                MetricCollector('linkCount', 'Number of links to router', ['address', 'container']),
                MetricCollector('unsettledCount', 'Number of unsettled messages', ['address']),
                MetricCollector('deliveryCount', 'Number of delivered messages', ['address'], "COUNTER"),
                MetricCollector('releasedCount', 'Number of released messages', ['address'], "COUNTER"),
                MetricCollector('rejectedCount', 'Number of rejected messages', ['address'], "COUNTER"),
                MetricCollector('acceptedCount', 'Number of accepted messages', ['address'], "COUNTER"),
                MetricCollector('undeliveredCount', 'Number of undelivered messages', ['address']),
                MetricCollector('capacity', 'Capacity of link', ['address'])
            ],
            self.get_connections: [
                MetricCollector('connectionCount', 'Number of connections to router', ['container']),
            ]}

    def get_router(self):
        return self.collect_metric('org.apache.qpid.dispatch.router')

    def get_links(self):
        links = self.collect_metric('org.apache.qpid.dispatch.router.link')
        connections = self.get_connections()

        for link in links:
            link["address"] = clean_address(link["owningAddr"])
            link["linkCount"] = 1
            link["container"] = get_container_from_connections(link["connectionId"], connections)

        return links

    def get_connections(self):
        response = self.collect_metric('org.apache.qpid.dispatch.connection')
        for result in response:
            result["connectionCount"] = 1
        return response

    def collect(self):
        collectormap = self.get_collectormap()
        for collectorfn in collectormap:
            entities = collectorfn()
            for collector in collectormap[collectorfn]:
                for entity in entities:
                    labels = []
                    for l in collector.labels:
                        if entity[l] != None:
                            labels.append(entity[l])
                    value = entity[collector.name]
                    if len(labels) == len(collector.labels):
                        collector.add(labels, int(value))
                yield collector.metric()
        

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
                return to_map(response)
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
