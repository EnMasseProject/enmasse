# Address controller

The address controller is about more than controller addresses. The main tasks performed by the address controller are:

    * API server for managing instances, addresses and flavors (HTTP and AMQP)
    * Implementation of Open Service Broker API for EnMasse
    * Controller for taking action based on instances (create/destroy enmasse infrastructure and managing routing + certs)
      etc.)
    * Controller for taking action based on addresses (create/destroy deployments for addresses pointing to flavors for brokers for instance)

## API server

The API server provides both an HTTP and AMQP API for creating/deleting instances and addresses
within those instances.

### Instance API

The Instance API is an external API for managing the instances in EnMasse. An instance is an
isolated instance of the messaging infrastructure with its own address space. The Instance API is
available whenever the address controller is deployed when the environment contains `MULTIINSTANCE=true`.

#### Resource kinds

There are two resource kinds in the Instance API, Instance and InstanceList. There is no formal
specification yet, but following is an example of each.

##### Instance

```
{
    "apiVersion": "v3",
    "kind": "Instance",
    "metadata": {
        "name": "tenant-a"
    },
    "spec": {
        "namespace": "mytenantnamespace",
        "messagingHost": "messaging.example.com",
        "mqttHost": "mqtt.example.com",
        "consoleHost": "console.example.com"
    },
    "status": {
        "ready": true
    }
}
```

The `status` field is populated by the address controller and should not be set by the client.

Any of the fields in the `spec` object may be omitted, but they have defaults:

    * namespace: a namespace on the form `enmasse-$name` is created. 
    * messagingHost: host taken by hostname generated for the `messaging` route
    * mqttHost : host taken by hostname generated for the `mqtt` route
    * consoleHost: host taken by hostname generated for the `console` route

##### InstanceList

```
{
    "apiVersion": "v3",
    "kind": "InstanceList",
    "items": [
        {
            "metadata": {
                "name": "tenant-a"
            },
            "spec": {
                "namespace": "mytenantnamespace",
                "messagingHost": "messaging.example.com",
                "mqttHost": "mqtt.example.com",
                "consoleHost": "console.example.com"
            },
            "status": {
                "ready": true
            }
        },
        ...
    ]
}
```

#### HTTP

| Method | Path                    | Consumes        | Produces        | Description                                                |
| ------ | ----------------------- | --------------- | --------------- | ---------------------------------------------------------- |
| GET    | /v3/instance            | N/A             | v3.InstanceList | Retrieves the list of all instances                        |
| GET    | /v3/instance/[instance] | N/A             | v3.Instance     | Retrieves the definition of a single instance              |
| PUT    | /v3/instance            | v3.InstanceList | v3.InstanceList | Replaces the entire instance config with provided config   |
| POST   | /v3/instance            | v3.Instance     | v3.InstanceList | Appends an instance to the list of instances               |
| DELETE | /v3/instance/[instance] | N/A             | v3.InstanceList | Deletes an instance and returns the remaining instances    |


#### AMQP

The AMQP API for creating instances does not yet exist.

### Addressing API

The Addressing API is an external API for managing the address space in EnMasse.

#### Resource kinds

There are two resource kinds in the Addressing API, Address and AddressList. There is no formal
specification yet, but following is an example of each.

##### Address

```
{
    "apiVersion": "v3",
    "kind": "Address",
    "metadata": {
        "name": "queue1"
    },
    "spec": {
        "store_and_forward": true,
        "multicast": false,
        "flavor": "vanilla-queue",
        "group_id": "optional-group-id"
    }
}
```

##### AddressList

```
{
    "apiVersion": "v3",
    "kind": "AddressList",
    "items": [
        {
            "metadata": {
                "name": "queue1"
            },
            "spec": {
                "store_and_forward": true,
                "multicast": false,
                "flavor": "small-queue"
            }
        },
        ...
    ]
}
```

#### HTTP

| Method | Path                  | Consumes       | Produces       | Description                                                                   |
| ------ | --------------------- | -------------- | -------------- | ----------------------------------------------------------------------------- |
| GET    | /v3/address           | N/A            | v3.AddressList | Retrieves the list of all addresses                                           |
| GET    | /v3/address/[address] | N/A            | v3.Address     | Retrieves the definition of a single address                                  |
| PUT    | /v3/address           | v3.AddressList | v3.AddressList | Replaces the entire addressing config with provided config                    |
| POST   | /v3/address           | v3.Address     | v3.AddressList | Appends an address to the list of addresses                                   |
| DELETE | /v3/address/[address] | N/A            | v3.AddressList | Deletes an address and returns the remaining addresses                        |

#### AMQP

The special address '$address' is used for attaching the sender link. The receiver link may be
dynamic, and the address given on attach should be used as the replyTo address in the request
message.

The request message contains an application property 'method' which is set to one of GET, PUT, POST or DELETE. An additional property, 'address' can be set to indicate the operation being performed on a single address. The following table shows the supported combinations of the 'method' property along with the presense of the 'address' property.

| method | address (true if set, false if not)    | Request body   | Response body  | Description                                                                   |
| ------ | -------------------------------------- | -------------- | -------------- | ----------------------------------------------------------------------------- |
| GET    | false                                  | N/A            | v3.AddressList | Retrieves the list of all addresses                                           |
| GET    | true                                   | N/A            | v3.Address     | Retrieves the definition of a single address                                  |
| PUT    | false                                  | v3.AddressList | v3.AddressList | Replaces the entire addressing config with provided config                    |
| POST   | false                                  | v3.Address     | v3.AddressList | Appends an address to the list of addresses                                   |
| POST   | false                                  | v3.AddressList | v3.AddressList | Appends a list of addresses to the list of addresses                          |
| DELETE | true                                   | N/A            | v3.AddressList | Deletes an address and returns the remaining addresses                        |

## Open Service Broker API

TODO

## Instance controller

TODO

## Flavor controller

TODO

## Address controller

TODO
