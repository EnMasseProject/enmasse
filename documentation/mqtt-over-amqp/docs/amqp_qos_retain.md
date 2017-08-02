# AMQP QoS and retain

The following document describes a proposal for defining the meaning of the QoS level and retain flag on messages moving
from the MQTT to AMQP specification. This concepts are already used today in the MQTT gateway and LWT services for EnMasse
but they could be defined in a more general way as not related at MQTT but just at AMQP level, because we want to have a
more generic AMQP LWT service (or "Connectivity" service).
 
## QoS Level

## What happens in the MQTT land

With the MQTT protocol you can define an end-to-end delivery guarantee from the publisher to the subscriber through a broker.
The QoS is specified at :

* _subscription level_, when a subscriber subscribes to a topic asking for a desired QoS
* _message level_, when the publisher sends the message with such QoS specified in the message itself

At _subscription level_, the subscriber asks for a desired QoS on the topic and it'd like to receive messages on that topic published
with such QoS. As reply to the subscription request, the broker provides the "granted" QoS for the topic which could be
the same asked by the subscriber or lower than that (i.e. subscriber wants QoS 1 on topic "foo" but broker replies with
granted QoS 0 on topic "foo". Here the subscriber can decide to close the connection if it needs QoS 1).

At _message level_, it means that the publisher sends a message specifying the QoS inside it and this QoS drives the
acknowledgment steps with the broker; so it's just about the "contract" between publisher and broker.

For having an end-to-end delivery guarantee, both the QoS levels play their rule.
When the broker has to re-publish a message published with QoS X to a subscriber which is subscribed with QoS Y, the following
criteria is applied :

`re-publish QoS = min(published QoS, granted QoS)`

In this case the re-publish QoS will be the QoS specified at _message level_ from broker to subscriber which will drive
the acknowledgment steps between them.

## What it could mean in the AMQP land

With the AMQP protocol the "settlement" is defined between the two peers (during link attachment with "modes" and on message with "settled") 
and how publisher sends the message to a broker doesn't effect how this message flows from broker to receiver.

> It's something different from the MQTT case where the re-publish QoS is influenced by how the publisher sends and how the 
subscriber wants to receive.

In AMQP the QoS could be specified at :

* _link level_ , when the receiver attaches a link specifying the desired QoS
* _message level_, when the sender sends the message with such QoS specified in the message itself

At _link level_, we could have :

* a new "desired-capabilities" named `qos-policy` during the link attachment from the receiver
* a link "property" specifying the value for the above `qos-policy`
* the source should support such "capability"
* a link "property" specifying the value for the desired QoS on receiving

The `qos-policy` could be something like :

* _exact_ : the receiver wants messages to be transferred to it using the same QoS as used in the original transfer from publisher to intermediary
* _enforce minimum_ : the receiver wants messages to be transferred to it using `QoS = min(published QoS, requested QoS)`
* _enforce maximum_ : the receiver wants messages to be transferred to it using `QoS = max(published QoS, requested QoS)`

> In this scenario, the MQTT over AMQP should work using the _enforce minimum_ 

At _message level_, it means that the sender sends a message specifying the "original" QoS for the message in the message 
itself using a "message annotations" like `x-opt-qos` where the values are :

* _0, AT MOST ONCE_
* _1, AT LEAST ONCE_
* _2, EXACTLY ONCE_

> The QoS levels are just the same as MQTT

## Retain

## What happens in the MQTT land

An MQTT publisher can specify the "retain" flag in the message which will be retained by the broker on the publishing topic.
It means that, if a new subscriber subscribes to such a topic, the retained message will be the first received message 
(it's also known as "last known value" on the topic).

The retain message is per-topic and it can be deleted publishing an empty message on that topic.

## What it could mean in the AMQP land

With AMQP protocol we could have :

* a "delivery annotation" named `x-opt-retain` on the sent message
* a "capability" on the target which describes that it can retain messages

Here the problem is how to encode an "empty" message for deleting the last retain message because as AMQP 1.0 spec :

"The body consists of one of the following three choices: one or more data sections, one or more amqp-sequence
sections, or a single amqp-value section."


