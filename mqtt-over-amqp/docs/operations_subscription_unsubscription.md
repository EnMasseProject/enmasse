# Subscription/Unsubscription

The MQTT client sends a _SUBSCRIBE_ message to FE. The QoS is represented by the “snd-settle-mode” and “rcv-settle-mode” in the attach performative.

* QoS 0 :
  * snd-settle-mode : settled (1)
* QoS 1 :
  * rcv-settle-mode : first (0)
  * snd-settle-mode : unsettled (0)
* QoS 2 :
  * rcv-settle-mode: second (1)
  * snd-settle-mode : unsettled (1)

The FE maps the _SUBSCRIBE_ message to the following AMQP message :

* **AMQP_SUBSCRIBE** : sent to the SS in order to ask establishing a route between the topics and unique client address $mqtt.to.[client-id]
  * subject : subscribe
  * topics (list)
  * reply-to : $mqtt.to.[client-id]
  * qos (list) (is it needed in order to define the QoS for the establihed route ?)

After sending the _AMQP_SUBSCRIBE_, the FE should receive the following messages as reply :

* **AMQP_SUBACK** : the result of a subscription request
  * subject : suback
  * return-codes (list)

The FE could receive the following message as reply on the unique client address  :

* **AMQP_PUBLISH** : the retained message to deliver to the client (see “Publishing”)

> the retained message is sent to the unique client address and it's not published on the topic because only that client should receive the message (not all the other subscribers for the topic itself).

Finally, the FE builds the _SUBACK_ message as response for the MQTT client and eventually the _PUBLISH_ for a retained message.

When subscribed/attached, the FE receives published messages on the unique client address :

* $mqtt.to.[client-id]

The AMQP message is used by FE for building the _PUBLISH_ message to send to the MQTT client (see “Publishing”). The _PUBACK_ (QoS 1) or _PUBREC_/_PUBCOMP_ (QoS 2) are used by FE and related AMQP client for sending the disposition with right settlement.

![Subscribe](../images/07_subscribe.png)

The MQTT client sends an _UNSUBSCRIBE_ message to FE which maps to the following AMQP message :

* **AMQP_UNSUBSCRIBE** : sent to the SS in order to ask removing the established route between the topic and unique client address $mqtt.to.[client-id]
  * subject : unsubscribe
  * topic
  * reply-to : $mqtt.to.[client-id]

After sending the _AMQP_UNSUBSCRIBE_, the FE receives the following messages as reply :

* **AMQP_UNSUBACK** : the result of an unsubscription request
  * subject : unsuback

Finally, the FE builds the _UNSUBACK_ message as response for the MQTT client.

![Unsubscribe](../images/08_unsubscribe.png)
