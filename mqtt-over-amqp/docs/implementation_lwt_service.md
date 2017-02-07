# Last Will and Testament Service

A possible Implementation could be using a backed queue on a broker at following address :

* $will

When the WS receives an AMQP_WILL message (see “Connection”), it puts this message into $will shared for all clients.

When the WS detects the link detaching from the FE (see “Disconnection”), it gets the message from $will using a “filter” on client-id (correlated with the detached link name)  and then publish it to the “will-topic”.

If WS crashes, on restart, it should get all messages from the $will queue and publish them. After, it should start a timer and if a client (which has a “will” in the queue) doesn’t reconnect in time, the “will” information will be deleted.

Another possible implementation could be using a backed queue on a broker for each client :

* $will.[client-id]

When the WS receives an AMQP_WILL message (see “Connection”), it puts this message into the right $will.[client-id] queue. The WS can receive the AMQP_WILL even during client life for overwriting the current one (or just adding it if the client didn’t provide will on connection).

When the WS detects the link detaching from the FE (see “Disconnection”), it gets the message from the right queue $will.[client-id] and then publish it to the “will-topic”.

If WS crashes, on restart, it should get all messages from the $will.[client-id] queues and publish them. After, it should start a timer and if a client (which has a “will” in its queue) doesn’t reconnect in time, the “will” information will be deleted.
