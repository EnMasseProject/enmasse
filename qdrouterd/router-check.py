#!/usr/bin/env python
from __future__ import print_function, unicode_literals
import optparse
from proton import Message
from proton.handlers import MessagingHandler
from proton.reactor import Container, DynamicNodeProperties
import json
import os

class Client(MessagingHandler):
    def __init__(self, url, message):
        super(Client, self).__init__()
        self.counter = 0
        self.url = url
        self.message = message

    def on_start(self, event):
        if self.message:
            self.sender = event.container.create_sender(self.url)
            self.receiver = event.container.create_receiver(self.sender.connection, None, dynamic=True)
        else:
            self.receiver = event.container.create_receiver(self.url)

    def send_request(self):
        if self.receiver.remote_source.address and self.message:
            id = self.counter
            self.counter += 1
            req = Message(id=id, correlation_id=id, reply_to=self.receiver.remote_source.address, properties=self.message.properties, body=self.message.body)
            self.sender.send(req)

    def on_link_opened(self, event):
        if event.receiver == self.receiver:
            self.send_request()

    def on_message(self, event):
        self.response = event.message
        event.connection.close()

def request(address, message):
    client = Client(address, message)
    container = Container(client)
    container.run()
    return client.response
 
def verify_router_config(address_config, router_config):
    for address in address_config:
        address_definition = address_config[address]
        if address_definition["store_and_forward"] == True and address_definition["multicast"] == False:
            found = False
            results = router_config["results"]
            for entry in results:
                name = entry[0]
                if name == address:
                    found = True
                    break
            if not found:
                raise "Unable to find queue %s in config" % name

config_host = os.environ['CONFIGURATION_SERVICE_HOST']
config_port = os.environ['CONFIGURATION_SERVICE_PORT']
mgmtMessage = Message(properties={"operation": "QUERY", "type": "org.amqp.management", "entityType": "org.apache.qpid.dispatch.router.config.address", "name": "self"}, body={"attributeNames": []})

address_config = json.loads(request("%s:%s/maas" % (config_host, config_port), None).body)
router_config = request("127.0.0.1:5672/$management", mgmtMessage).body

verify_router_config(address_config, router_config)
