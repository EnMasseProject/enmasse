This repository contains all components which provide [MQTT](http://mqtt.org) protocol support in EnMasse.

# MQTT gateway

MQTT gateway component for EnMasse which provides connectivity to remote MQTT clients. Through this components, MQTT clients can connect to EnMasse in order to subscribe to topics for receiving published messages; at same time they can publish messages to topics as well.

# MQTT Last Will and Testament service

MQTT Last Will and Testament service component for EnMasse which provides "will" messages handling for remote MQTT clients. Through this components, the "will" information sent by an MQTT client on connection are stored; when the client disconnects not in the clean way (DISCONNECT message), this components provides to send the "will" message to all the MQTT subscribed client.

Both the components are implemented following the specification defined by the documentation for bringing [MQTT over AMQP](../documentation/mqtt-over-amqp)
