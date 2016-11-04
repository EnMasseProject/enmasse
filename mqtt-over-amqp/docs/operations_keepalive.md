# Keep alive

The keep alive mechanism at MQTT side is handled by the FE, exchanging PINGREQ and PINGRESP with the MQTT client.

The life of the corresponding AMQP client is handled by AMQP “empty” message at connection level (as usual).

> multiple AMQP clients can “live” with same AMQP connection so with only one AMQP “empty” keep alive message (i.e. using a router to establish the connection to the broker/container).
