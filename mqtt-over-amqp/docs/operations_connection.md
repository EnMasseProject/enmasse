# Connection

The MQTT client sends a CONNECT message to the FE which maps the message to the following AMQP messages :

* AMQP_WILL : sent to the WS in order to provide “will” information
  * subject : will-message
  * will-retain
  * will-qos
  * will-topic
  * client-id (could be useless because WS can use attached link-name for that)
  * payload (the “will” content)
* AMQP_SESSION : sent to the SS in order to provide “clean-session” information and querying for a previous session
  * subject : session
  * client-id
  * clean-session
  * reply-to : $mqtt.to.<client-id>

After sending the AMQP_SESSION, the FE receives the following message as reply :

* AMQP_SESSION_PRESENT : sent by the SS to report to FE if a session is already present for the client-id
  * subject : session-present
  * session-present

If a session is present and there are subscriptions for the client-id, the SS re-establishes the routes from each topic to the $mqtt.to.<client-id> (see “Subscription/Unsubscription”) automatically. No need for the SS to send subscriptions list to the FE in order to re-subscribe.

> the SS should send the AMQP_SESSION_PRESENT message to FE before re-establishing routes for the topics because it should be first message received by FE (in order to build the CONNACK) if there are messages stored when the client was offline that will be puslished now on re-connect.

Regarding the WS, the FE attaches a permanent link to the $mqtt.willservice address; the WS maps the link-name with the “will” information provided in order to have a 1-1 relationship between them. See “Disconnection”, in order to know how publishing “will” information could happen on client disconnection.

> the AMQP_WILL message can be sent by the FE even during client life (not only on connection). It means to overwrite the last “will” information (something that doesn’t exist in the MQTT spec).

The FE builds the MQTT CONNACK message and sends it to the MQTT client.
