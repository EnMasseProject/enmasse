# MQTT Last Will and Testament service

MQTT Last Will and Testament service component for EnMasse which provides "will" messages handling for remote MQTT clients. Through this components, the "will" information sent by an MQTT client on connection are stored; when the client disconnects not in the clean way (DISCONNECT message), this components provides to send the "will" message to all the MQTT subscribed client.

This implementation follow the specification defined by the following documentation for bringing [MQTT over AMQP](../../documentation/mqtt-over-amqp)
