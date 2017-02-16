# Last Will and Testament Service

## API

It's possible to define an API for interacting with something like an LWT Storage Service which can handle the underlying way for storing and retrieving "will" information.

This API is just made of basic CRUD (Create, Read, Update and Delete) operations :

* Create : for adding a "will" message of a new connected client
* Read : for getting the "will" message of a connected client
* Update : for updating the "will" message of a connected client
* Delete : for deleting the "will" message of a connected client

> The "Update" operation doesn't make sense in the MQTT space, because for this protocol "will" information can be provided only on connection. Updating "will" message is available for using this service with native AMQP clients where LWT feature doesn't exist by specification and is built on top of the raw protocol itself.

### Create

*Request*

| DATA | TYPE | DESCRIPTION |
| ---- | ---- | ----------- |
| client-id | String | Client identifier (or link name) related to "will" information to store |
| will retain flag | Boolean | if the "will" is retained |
| will QoS level | Integer | QoS level for "will" publishing |
| will topic | String | Topic on which the "will" should be published |
| will message | Bytes | Payload of the "will" message |

*Response*

| DATA | TYPE | DESCRIPTION |
| ---- | ---- | ----------- |
| response-code | Integer | Result for the requested operation |

### Read

*Request*

| DATA | TYPE | DESCRIPTION |
| ---- | ---- | ----------- |
| client-id | String | Client identifier (or link name) related to "will" information to get |

*Response*

| DATA | TYPE | DESCRIPTION |
| ---- | ---- | ----------- |
| response-code | Integer | Result for the requested operation |
| client-id | String | Client identifier (or link name) related to "will" information |
| will retain flag | Boolean | if the "will" is retained |
| will QoS level | Integer | QoS level for "will" publishing |
| will topic | String | Topic on which the "will" should be published |
| will message | Bytes | Payload of the "will" message |

### Update

*Request*

| DATA | TYPE | DESCRIPTION |
| ---- | ---- | ----------- |
| client-id | String | Client identifier (or link name) related to "will" information to update |
| will retain flag | Boolean | if the "will" is retained |
| will QoS level | Integer | QoS level for "will" publishing |
| will topic | String | Topic on which the "will" should be published |
| will message | Bytes | Payload of the "will" message |

*Response*

| DATA | TYPE | DESCRIPTION |
| ---- | ---- | ----------- |
| response-code | Integer | Result for the requested operation |

### Delete

*Request*

| DATA | TYPE | DESCRIPTION |
| ---- | ---- | ----------- |
| client-id | String | Client identifier (or link name) related to "will" information to delete |

*Response*

| DATA | TYPE | DESCRIPTION |
| ---- | ---- | ----------- |
| response-code | Integer | Result for the requested operation |

A possible implementation could be just using a database for backing "will" messages.

## Interactions

When the LWTS receives an AMQP_WILL message (see “Connection”), it executes a "create" operation on the LWT Storage Service in order to store the "will" information for the new connected client.
If the AMQP_WILL message is received by an already connected client, the operation to be executed is an "update" for the "will" information already stored.

When the LWTS detects the link detaching from the GW (see “Disconnection”), it can do the following :

* if it's a clean detaching (no error), then a "delete" operation is executed against the LWT Storage Service just for deleting "will" information for the disconnected client because the "will" don't need to be published
* if it's a brute detaching (error), then a "read" operation is executed against the LWT Storage Service in order to get the "will" information for the disconnected client. Then, this information are used for building an AMQP_PUBLISH message to publish on the "will topic" and finally a "delete" operation is executed

If LWT service crashes, on restart, it should get all messages from the LWT Storage Service and publish them. After, it should start a timer and if a client (which has a “will” stored) doesn’t reconnect in time, the “will” information will be deleted.
