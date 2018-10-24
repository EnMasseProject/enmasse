from __future__ import print_function, unicode_literals
import optparse
from proton import Message
from proton.handlers import MessagingHandler
from proton.reactor import Container

class HelloWorld(MessagingHandler):
    def __init__(self, url):
        super(HelloWorld, self).__init__()
        self.url = url

    def on_start(self, event):
        event.container.create_receiver(self.url)
        event.container.create_sender(self.url)

    def on_sendable(self, event):
        event.sender.send(Message(body="Hello World!"))
        event.sender.close()

    def on_message(self, event):
        print("Received: " + event.message.body)
        event.connection.close()

parser = optparse.OptionParser(usage="usage: %prog [options]")
parser.add_option("-u", "--url", default="amqps://localhost:5672/myqueue",
                  help="url to use for sending and receiving messages")
opts, args = parser.parse_args()

try:
    Container(HelloWorld(opts.url)).run()
except KeyboardInterrupt: pass
