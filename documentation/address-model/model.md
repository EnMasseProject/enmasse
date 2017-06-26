# *PROPOSAL* Address model for EnMasse

This document defines the EnMasse address model. This involves the types of address spaces, the
types of addresses within each address space, and the available plans.

## Definitions

### Address space

An Address Space is a group of addresses that can be accessed through a single connection. This
means that clients connected to the endpoints of an address space may send or receive messages to any address
it is _authorized_ to send or receive messages to.

### Address

An Address is part of an Address Space, and represents a destination used for sending and receiving
messages. An address has a type, which defines the semantics of sending and receiving messages
to/from that address. Each address may also support a set of properties that may change the
semantics and/or control who may interact with that address.

### Plans

Both `address spaces` and `addresses` may be restricted by a plan, which typically translates to a
limit on resource usage in different dimensions.

#### Address Space Plans

Each address space has plan that restricts the aggregated resource usage within an address space.
There are 3 types of address space plans:

   * small
   * medium
   * large

The set of plans may be extended in the future. Each address space type may translate the plan into a set of restrictions on the infrastructure such as 'scales up to 5 routers' or 'may create up to 10 addresses'. These restrictions are documented within each address space.


#### Address Plans

The usage of each address is also constrained by a plan. There are 3 types of plans:

   * small
   * medium
   * large

The set of plans may be extended in the future. Each address type maybe translate the plan into a
set of restrictions such as 'up to 5 consumers' or 'up to 100 messages/hour'. The restrictions of a
plan is documented within each address type.

## Address spaces

The currently supported address spaces are:

   * standard

### Standard address space

The `standard` address space is the default in EnMasse, and consists of an AMQP router network in
combination with attachable 'storage units'. The implementation of a storage unit is hidden from
the client and the routers with a well defined API. This address space type is appropriate when you
need to support many connections and addresses, but requires message ordering to be enforced by clients. 

Clients connect and send/receive messages in this address space using the [AMQP](www.amqp.org) protocol.

#### Address types

The standard address space supports 4 address types:

   * queue
   * topic
   * anycast
   * broadcast

##### Queue

The `queue` address type is a scalable store-and-forward queue. This address type is appropriate for
implementing a distributed work queue, handling traffic bursts and other usecases where you want to
decouple the producer and consumer. A queue may be sharded across multiple storage units, in which case
message order is no longer guaranted

##### Topic

The `topic` address type supports the publish-subscribe messaging pattern where you have 1..N
producers and 1..M consumers. Each message published to a topic address is forwarded to all
subscribers for that address. A subscriber may also be durable, in which case messages are kept
until the subscriber has acknowledged them.

##### Anycast

The `anycast` address type is a scalable 'direct' address for sending messages to one consumer. Messages sent to an `anycast` address is
not stored but forwarded directly to the consumer. This makes this address type ideal for
request-reply (RPC) uses or even work distribution. This is the cheapest address type as it doesn't
require any persistence.

##### Broadcast

The `broadcast` address type is a scalable 'direct' address for sending messages to multiple
consumers. Messages sent to a `broadcast` address are forwarded to all consumers receiving on that
address. It is important to note that only pre-settled messages can be sent to broadcast addresses,
as message acknowledgements from consumers are not propagated to producers.


