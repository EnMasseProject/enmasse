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
from proton import Message, Url, ConnectionException, Timeout, SSLDomain
from proton.utils import SyncRequestResponse, BlockingConnection
from proton.handlers import IncomingMessageHandler
import sys
from prometheus_client import start_http_server, Gauge
import random
import time
import os
from prometheus_client.core import GaugeMetricFamily, CounterMetricFamily, REGISTRY

class MetricCollector(object):
    def __init__(self, name, description, labels, mtype="GAUGE", id=None, filter=None):
        self.name = name
        if id == None:
            self.id = name
        else:
            self.id = id
        self.description = description
        self.labels = labels
        self.filter = filter
        self.label_list = []
        self.value_list = []
        if mtype == "GAUGE":
            self.metric_family = GaugeMetricFamily(self.id, self.description, labels=self.labels)
        elif mtype == "COUNTER":
            self.metric_family = CounterMetricFamily(self.id, self.description, labels=self.labels)
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

class RouterResponse(object):
    def __init__(self, response):
        self.response = response

    def get_index(self, attribute):
        try:
            return self.response.body["attributeNames"].index(attribute)
        except ValueError:
            return None

    def get_results(self):
        return self.response.body["results"]

    def add_field(self, name, value):
        self.response.body["attributeNames"].append(name)
        for result in self.response.body["results"]:
            result.append(value)

    def add_field_from(self, name, from_field, transform):
        from_idx = self.response.body["attributeNames"].index(from_field)
        self.response.body["attributeNames"].append(name)
        for result in self.response.body["results"]:
            result.append(transform(result[from_idx]))

    def contains(self, result, filter):
        if filter == None:
            return True
        for key, value in filter.iteritems():
            key_idx = self.get_index(key)
            if result[key_idx] != value:
                return False
        return True

def clean_address(address):
    if address == None:
        return address
    elif address[0] == 'M':
        return address[2:]
    else:
        return address[1:]

def get_container_from_connections(connection_id, connections):
    container_idx = connections.get_index("container")
    id_idx = connections.get_index("identity")
    for connection in connections.get_results():
        if connection_id == connection[id_idx]:
            return connection[container_idx]
    return None

class RouterCollector(object):

    def __init__(self, router_host, router_port, cert_dir):
        self.router_host = router_host
        self.router_port = router_port
        self.cert_dir = cert_dir
        ssl_domain = None
        allowed_mechs = []
        sasl_enabled = False

        if self.cert_dir != None:
            ssl_domain = SSLDomain(SSLDomain.MODE_CLIENT)
            ssl_domain.set_trusted_ca_db(str(os.path.join(self.cert_dir, 'ca.crt')))
            ssl_domain.set_credentials(str(os.path.join(self.cert_dir, "tls.crt")), str(os.path.join(self.cert_dir, "tls.key")), None)
            ssl_domain.set_peer_authentication(SSLDomain.VERIFY_PEER)
            allowed_mechs = str("EXTERNAL")
            sasl_enabled = True

        self.client = SyncRequestResponse(BlockingConnection("amqps://" + self.router_host + ":" + str(self.router_port), 30, None, ssl_domain, allowed_mechs=allowed_mechs, sasl_enabled=sasl_enabled), "$management")

    def create_collector_map(self):
        metrics = [ MetricCollector('connectionCount', 'Number of connections to router', ['container']),
                    MetricCollector('connectionCount', 'Total number of connections to router', ['routerId'], id="totalConnectionCount"),
                    MetricCollector('linkCount', 'Number of links to router', ['address']),
                    MetricCollector('linkCount', 'Number of consumers to router', ['address'], id="consumerCount", filter={"linkDir": "out"}),
                    MetricCollector('linkCount', 'Number of producers to router', ['address'], id="producerCount", filter={"linkDir": "in"}),
                    MetricCollector('linkCount', 'Total number of links to router', ['routerId'], id="totalLinkCount"),
                    MetricCollector('addrCount', 'Number of addresses defined in router', ['routerId']),
                    MetricCollector('autoLinkCount', 'Number of auto links defined in router', ['routerId']),
                    MetricCollector('linkRouteCount', 'Number of link routers defined in router', ['routerId']),
                    MetricCollector('unsettledCount', 'Number of unsettled messages', ['address']),
                    MetricCollector('deliveryCount', 'Number of delivered messages', ['address'], "COUNTER"),
                    MetricCollector('releasedCount', 'Number of released messages', ['address'], "COUNTER"),
                    MetricCollector('rejectedCount', 'Number of rejected messages', ['address'], "COUNTER"),
                    MetricCollector('acceptedCount', 'Number of accepted messages', ['address'], "COUNTER"),
                    MetricCollector('undeliveredCount', 'Number of undelivered messages', ['address']),
                    MetricCollector('capacity', 'Capacity of link', ['address']) ]
        m = {}
        for metric in metrics:
            m[metric.id] = metric
        return m

    def create_entity_map(self, collector_map):
        return { self.get_router: [collector_map['totalConnectionCount'], collector_map['totalLinkCount'],
                                   collector_map['addrCount'], collector_map['autoLinkCount'], collector_map['linkRouteCount']],
                 self.get_connections: [collector_map['connectionCount']],
                 self.get_links: [collector_map['linkCount'], collector_map['unsettledCount'],
                                  collector_map['deliveryCount'], collector_map['releasedCount'],
                                  collector_map['rejectedCount'], collector_map['acceptedCount'],
                                  collector_map['undeliveredCount'], collector_map['capacity'],
                                  collector_map['consumerCount'], collector_map['producerCount']] }

    def get_router(self):
        return self.collect_metric('org.apache.qpid.dispatch.router')

    def get_links(self):
        links = self.collect_metric('org.apache.qpid.dispatch.router.link')
        if links == None:
            return links

        connections = self.get_connections()
        if connections == None:
            return connections

        links.add_field_from("address", "owningAddr", clean_address)
        links.add_field("linkCount", 1)
        links.add_field_from("container", "connectionId", lambda connection_id: get_container_from_connections(connection_id, connections))

        return links

    def get_connections(self):
        response = self.collect_metric('org.apache.qpid.dispatch.connection')
        if response == None:
            return response

        response.add_field("connectionCount", 1)
        return response

    def collect(self):
        collector_map = self.create_collector_map()
        fetcher_map = self.create_entity_map(collector_map)

        for fetcher in fetcher_map:
            response = fetcher()
            if response != None:
                for collector in fetcher_map[fetcher]:
                    for entity in response.get_results():
                        if response.contains(entity, collector.filter):
                            labels = []
                            for l in collector.labels:
                                label_idx = response.get_index(l)
                                if label_idx != None and entity[label_idx] != None:
                                    labels.append(entity[label_idx])
                                else:
                                    labels.append("")
                            value = entity[response.get_index(collector.name)]
                            collector.add(labels, int(value))

        for collector in collector_map.itervalues():
            yield collector.metric()
        

    def collect_metric(self, entityType):
        try:
            properties = {}
            properties["entityType"] = entityType
            properties["operation"] = "QUERY"
            properties["name"] = "self"
            message = Message(body=None, properties=properties)
            response = self.client.call(message)
            if response == None:
                return response
            else:
                return RouterResponse(response)
        except NameError as e:
            print("Error querying router for metrics: %s" % e)
            return None

if __name__ == '__main__':
    # Start up the server to expose the metrics.
    REGISTRY.register(RouterCollector(os.environ['ROUTER_HOST'], int(os.environ['ROUTER_PORT']), os.environ['CERT_DIR']))
    start_http_server(8080)
    while True:
        time.sleep(5)
