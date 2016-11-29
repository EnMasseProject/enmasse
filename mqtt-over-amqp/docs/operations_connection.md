# Connection

## FE attached links

The FE needs to attach a link with sender role to the Will Service control address. It should have QoS as AT_LEAST_ONCE so with :

* rcv-settle-mode : first (0)
* snd-settle-mode : unsettled (0)

The FE needs to attach a link with sender role to the Subscription Service control address. It should have QoS as AT_LEAST_ONCE so with :

* rcv-settle-mode : first (0)
* snd-settle-mode : unsettled (0)

The FE needs to attach a link with receiver role to the unique client address $mqtt.to.[client-id]. It should have QoS as AT_LEAST_ONCE so with :

* rcv-settle-mode : first (0)
* snd-settle-mode : unsettled (0)

or using "mixed" (2) for the "snd-settle-mode", in order to allow the sender on the other side to send messages both "settle" and "unsettled" on the same link.

## "Will" information to Will Service

The MQTT client sends a _CONNECT_ message to the FE which extracts "will" information for building the following AMQP message to send to the Will Service.

**AMQP_WILL** : sent to the WS in order to provide “will” information.

| DATA | TYPE | VALUE | FROM |
| ---- | ---- | ----- | ---- |
| subject | system property | "will" | - |
| x-retain | message annotation | will retain flag | MQTT CONNECT |
| x-qos | message annotation | will QoS level | MQTT CONNECT |
| durable | header | will QoS level > 0 | MQTT CONNECT |
| to | system property | will topic | MQTT CONNECT |
| payload | Data section | will message | MQTT CONNECT |

> the MQTT QoS level is copied to the x-qos annotation. At same time the "durable" header field is set as FALSE if QoS level is 0, TRUE if QoS level is 1 or 2. When the x-qos annotations isn't present (i.e. AMQP_WILL published from a native AMQP client which doesn't add it), fallback to use only "durable" and if it's TRUE consider QoS level 1 as default.

The _AMQP_WILL_ is sent as "unsettled", in order to know that the Will Service has received it (with related disposition).
The relation between the _AMQP_WILL_ message and the related client, at AMQP level, is inferred by the link name attached to the WS control address.

![Connect Will Service](../images/03_connect_ws.png)

The FE attaches a permanent link to the $mqtt.willservice address; the WS maps the link-name with the “will” information provided in order to have a 1-1 relationship between them. See “Disconnection”, in order to know how publishing “will” information could happen on client disconnection.

> the _AMQP_WILL_ message can be sent by the FE even during client life (not only on connection). It means to overwrite the last “will” information (something that doesn’t exist in the MQTT spec).

## "Session" request to Subscription Service

From the same received MQTT _CONNECT_ message, the FE extracts information related to session handling for building the following AMQP message to send to the Subscription Service.

**AMQP_SESSION** : sent to the SS in order to provide “clean-session” information and querying for a previous session.

| DATA | TYPE | VALUE | FROM |
| ---- | ---- | ----- | ---- |
| subject | system property | "session" | - |
| x-clean-session | message annotation | clean session flag | MQTT CONNECT |
| reply-to | system property | $mqtt.to.[client-id] | - |

The _AMQP_SESSION_ is sent as "unsettled", in order to know that the Subscription Service has received it (with related disposition).
The relation between the _AMQP_SESSION_ message and the related client, at AMQP level, is inferred by the link name attached to the SS control address.

> the [client-id] is the "client identifier" value from the MQTT CONNECT message.

After sending the _AMQP_SESSION_, the FE receives the following message as reply.

**AMQP_SESSION_PRESENT** : sent by the SS to report to FE if a session was already present and recovered.

| DATA | TYPE | VALUE | FROM |
| ---- | ---- | ----- | ---- |
| subject | system property | "session-present" | - |
| x-session-present | message annotation | if client session did already exist and routes were recovered | - |

The _AMQP_SESSION_PRESENT_ is sent as "unsettled", in order to know that the FE has received it (with related disposition).

If a session is present and there are subscriptions for the client-id, the SS re-establishes the routes from each topic to the $mqtt.to.[client-id] automatically (see “Subscription/Unsubscription”). No need for the SS to send subscriptions list to the FE in order to re-subscribe.

> the SS should send the _AMQP_SESSION_PRESENT_ message to FE before re-establishing routes for the topics because it should be first message received by FE (in order to build the _CONNACK_) if there are messages stored when the client was offline that will be puslished now on re-connect.

![Connect Subscription Service](../images/04_connect_ss.png)

## Reply to MQTT client

Based on the replies from Will Service and mostly Subscription Service, the FE builds the _CONNACK_ message and sends it to the MQTT client.

**CONNACK**

| DATA | VALUE | FROM |
| ---- | ----- | ---- |
| Session present | if sesson is already present | AMQP_SESSION_PRESENT  |
| Connect return code | It can depends on some checks on FE side or other on AMQP side | - |
