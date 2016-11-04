# Subscription Service

It could be based on the same service as in EnMasse project using brokers and durable subscription on topics.

On connection, the client with “clean session = false” want to know if a session is already present (MQTT spec). The SS can check if durable subscriptions already exist for that client (based on subscription name with client id). If a session is available the SS should recover the routes from subscriptions to the direct unique client address $mqtt.to.<client-id> so that “offline” messages are sent to the client.

When a message is published with a “retain” flag (related property in the related AMQP message), the SS could configure a forwarding of the message from the “topic” to a queue like $retain.<topic> which will store the retained message.

On subscription, the client provides to the SS the following information :

* its direct unique address, $mqtt.to.<client-id>
