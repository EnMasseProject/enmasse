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

from __future__ import print_function, unicode_literals
import optparse
from proton import Message, SSLDomain
from proton.handlers import MessagingHandler
from proton.reactor import Container

class Recv(MessagingHandler):
    def __init__(self, server, address, count, peer_hostname):
        super(Recv, self).__init__()
        self.server = server
        self.address = address
        self.expected = count
        self.received = 0
        self.peer_hostname = peer_hostname
        self.ssl_domain = SSLDomain(SSLDomain.MODE_CLIENT)

    def on_start(self, event):
        conn = event.container.connect(self.server, ssl_domain=self.ssl_domain, virtual_host=self.peer_hostname)
        event.container.create_receiver(conn, self.address)

    def on_message(self, event):
        if event.message.id and event.message.id < self.received:
            # ignore duplicate message
            return
        if self.expected == 0 or self.received < self.expected:
            print(event.message.body)
            self.received += 1
            if self.received == self.expected:
                event.receiver.close()
                event.connection.close()

parser = optparse.OptionParser(usage="usage: %prog [options]",
                               description="Send messages to the supplied address.")
parser.add_option("-c", "--connectHost", default="localhost:5672",
                  help="host to connect to (default %default)")
parser.add_option("-a", "--address", default="myaddress",
                  help="address to which messages are sent (default %default)")
parser.add_option("-s", "--servername", help="servername to set as SNI")
parser.add_option("-m", "--messages", type="int", default=100,
                  help="number of messages to send (default %default)")
opts, args = parser.parse_args()

try:
    Container(Recv(opts.connectHost, opts.address, opts.messages, opts.servername)).run()
except KeyboardInterrupt: pass
