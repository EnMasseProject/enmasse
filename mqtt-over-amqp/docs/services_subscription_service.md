# Subscription Service

The Subscription Service (SS) has an AMQP receiver on the following control address :

* $mqtt.subscriptionservice

It's able to handle following scenarios :

* receiving “clean-session” information in order to handle session for the client. If needed, the SS checks if a session already exists for the client-id in terms of subscriptions.
* receiving a request from a client for establishing a subscription for a topic. The SS has to ensure a route from “topic” to the unique client address $mqtt.to.<client-id> (NOTE : it's not in charge for delivering but only to ensure that a route is established).
* delivering the retained message if it’s available for the subscribed “topic”

The SS replies with the result of a subscription request to the following client specific address :

* $mqtt.to.<client-id>
