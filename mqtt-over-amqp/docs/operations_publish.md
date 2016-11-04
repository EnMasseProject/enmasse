# Publish

The MQTT client sends PUBLISH message to FE which maps the message to the following AMQP messages :

* AMQP_PUBLISH : sent to the “topic” address
  * to (topic from PUBLISH)
  * qos (redundant ? QoS influences exchange)
  * retain
  * payload
  * delivery-count (DUP from MQTT PUBLISH) → just value 1 ?

The requested QoS is represented by the “snd-settle-mode” and “rcv-settle-mode” in the attach performative.

* QoS 0 :
  * snd-settle-mode : settled (1)
* QoS 1 :
  * rcv-settle-mode : first (0)
  * snd-settle-mode : unsettled (0)
* QoS 2 :
  * rcv-settle-mode: second (1)
  * snd-settle-mode : unsettled (1)

For QoS 1, after disposition on the AMQP client, the FE sends the PUBACK to the MQTT client.

For QoS 2, we have intermediate PUBREC, PUBREL and PUBCOMP to send from FE to MQTT client.

> the FE doesn't detach the link on the topic for each publish; it leaves the link attached.

![Publish QoS 0](../images/09_publish_qos_0.png)

![Publish QoS 1](../images/10_publish_qos_1.png)
