This page documents the Addressing API

# Overview

The Addressing API is an external API for managing the address space in EnMasse. The API is support
both for HTTP and AMQP


# Resource objects

There are two resource objects in the Addressing API, Address and AddressList. There is no formal
specification yet, but following is an example of each.

## Address

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

## AddressList

```
{
    "apiVersion": "v3",
    "kind": "AddressList",
    "spec": {
        "queue1": {
            "store_and_forward": true,
            "multicast": false,
            "flavor": "small-queue"
        },
        "queue2": {
            "store_and_forward": true,
            "multicast": false,
            "flavor": "standard-queue",
            "group_id": "shared-group"
        },
        "queue2": {
            "store_and_forward": true,
            "multicast": false,
            "flavor": "standard-queue",
            "group_id": "shared-group"
        }
    }
}
```

# HTTP

| Method | Path                  | Consumes       | Produces       | Description                                                                   |
| ------ | --------------------- | -------------- | -------------- | ----------------------------------------------------------------------------- |
| GET    | /v3/address           | N/A            | v3.AddressList | Retrieves the list of all addresses                                           |
| GET    | /v3/address/[address] | N/A            | v3.Address     | Retrieves the definition of a single address                                  |
| PUT    | /v3/address           | v3.AddressList | v3.AddressList | Replaces the entire addressing config with provided config                    |
| POST   | /v3/address           | v3.Address     | v3.AddressList | Appends an address to the list of addresses                                   |
| POST   | /v3/address           | v3.AddressList | v3.AddressList | Appends a list of addresses to the list of addresses                          |
| DELETE | /v3/address/[address] | N/A            | v3.AddressList | Deletes an address and returns the remaining addresses                        |

# AMQP

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
