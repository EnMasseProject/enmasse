# subscription services

## link routed subscription location

For reconnecting with link routed durable subscriptions, the client's
link must be routed to the broker on which the subscription is
actually stored. The router itself does not have this information.

A subscription locator service is therefore provided in this
component. To us it establish a link to locate/<topic-name> or
locate.<topic-name>, where <topic-name> is the actual name of the
topic the subscription was created on. This link will then be detached
by the locator service with link error set to 'amqp:link:redirect' and
the 'address' field of the info map set to a qualified address that
the router can correctly route to the correct broker.

## message-driven control of message routed subscriptions

This component also offers a message-driven service for managing
subscriptions that are message routed. To use the service control
messages are sent to the '$subctrl' address. The subscriber should
create a receiving link on a unique address over which the messages
for any subscriptions will be delivered.

Each control message should have the correlation id set to this unique
address. The type of the control message is set as the subject of the
message. The control message will be accepted if successfully acted on
or else rejected with an error.

The following control subjects are supported:

* subscribe

The body should be a single topic, a list of topics or a map of topics
to 'tags'. The tags used in the map form can be any string useful to
the client (e.g. in MQTT these can be used to 'store' the desired QoS
along side the subscription.

On receipt of a subscribe request, the service will attempt to ensure
that messages from all the specified topics are queued and then
delivered reliably to the unique address (indicated in the correlation
id).

* unsubscribe

The body should be a single topic, a list of topics, or a map with the
topic as the key. (In the case of a map, the value is ignored). On
receipt of an unsubscribe request, the service will prevent further
message published to the specified topics from being stored and
delivered to the unique address (indicated in the correlation id).

* list

The reply-to address should be set, and the service will then send a
message to that address with the subject 'subscriptions' whose body is
a map whose keys are the topic names subscribed to under the
subscription identifier indicated by the correlation id of the
original request. The values will be any 'tag' originally specified in
the corresponding subscribe request, or 'true' if no such tag was
specified.

* close

This will terminate all subscriptions under the subscription
identifier as indicated by the correlation id.
