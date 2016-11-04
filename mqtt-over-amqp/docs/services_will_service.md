# Will Service

The Will Service (WS) has an AMQP receiver on the following control address :

* $mqtt.willservice

It's able to handle following scenarios :

* receiving “will” information for a new connected client (see “Connection”)
* start publishing “will” due to a brute client disconnection (see “Disconnection”). The WS acts as an AMQP sender on “will-topic” in order to publish the “will”.
* removing “will” information for a specific client (see “Disconnection”)
* overwriting “will” information for a specific client (it’s something that we don’t have in the MQTT spec).
