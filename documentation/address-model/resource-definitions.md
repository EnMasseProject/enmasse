# EnMasse resource definition

This document defines the resources consumed and produced by APIs and can be considered a base for writing the schema for the data formats. The [address model](model.md) defines address spaces and addresses which translates to these resources.

The resources defined herein may be created, deleted and updated through different APIs. Either the
Open Service Broker API, the custom HTTP API, or using AMQP management.

The resources are defined in the JSON format. 

## AddressSpace

The `AddressSpace` resource defines the type of address space and endpoint for connecting.

```
{
    "apiVersion": "enmasse.io/v1",
    "kind": "AddressSpace",
    "metadata": {
        "name": "myspace" // Required
    },
    "spec": {
        "type": "standard", // Optional. Default is 'standard'
        "plan": "small", // Required. This is the aggregate 'size' of the address space (i.e. controls how
                         // many addresses and how many resources each address may consume
        "endpoints": [ // Optional. If not provided, the default generated hostnames will be provided
            {
                "type": "messaging", // Required. The available types are defined by the address space type
                "host": "messaging.example.com", // Optional. The host name for this endpoint to use for routing by the load balancer
                "cert": { // Optional. Self-signed certificates are generated if not specified
                    "provider": "secret", // Provider of certs may be through secrets or ACME. For 'secret', the secret name must be passed
                    "secretName": "mysecret"
                }
            },
            {
                "type": "mqtt",
                "host": "mqtt.example.com"
            },
            {
                "type": "console",
                "host": "console.example.com"
            }
        ]
    }
}
```

## Address

The `Address` resource defines an address and its semantics.

```
{
    "apiVersion": "enmasse.io/v1", // Required
    "kind": "Address", // Required
    "metadata": {
        "name": "myqueue", // Required
        "address_space": "myspace" // Optional. Defaults to 'default' address space
    },
    "spec": {
        "address": "my_queue", // Optional. Default is .metadata.name
        "type": "queue", // Required. Valid types are defined by the AddressSpace
        "properties": { // Optional
        },
        "plan": "medium" // Required. 
    }
}
```

