# EnMasse on a local development instance of OpenShift

This guide will walk through the process of setting up EnMasse on OpenShift on a local developemnt
cluster together with clients for sending and receiving messages.

## Preqrequisites

In this guide, you need the OpenShift client tools.  You can download the [OpenShift Origin](https://github.com/openshift/origin/releases) client for this guide. EnMasse will work with the latest stable release.

Follow [this guide](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md) for setting up a local developer instance of OpenShift.

## Setting up EnMasse

### Creating project and importing template

EnMasse comes with a few templates that makes setting it up easy. First, create a new project:

    oc new-project enmasse

Then download a script for deploying the EnMasse template:

    curl -o enmasse-deploy.sh https://raw.githubusercontent.com/EnMasseProject/openshift-configuration/master/scripts/enmasse-deploy.sh

This script simplifies the process of deploying the enmasse cluster to your openshift instance. You can invoke it with `-h` to get a list of options.

### Creating the EnMasse instance

Now you are ready for creating the messaging service itself:

    bash enmasse-deploy.sh -c "https://localhost:8443" -p enmasse

This will create the deployments required for running EnMasse. Starting up EnMasse will take a while, usually depending on how fast it is able to download the docker images for the various components.  In the meantime, you can start to create your address configuration.

### Configuring addresses 

EnMasse is configured with a set of addresses that you can use for messages. Currently, EnMasse supports 4 different address types:

   * Brokered queues
   * Brokered topics (pub/sub)
   * Direct anycast addresses 
   * Direct broadcast addresses

Here is an example config with all 4 variants that you can save to `addresses.json`:

```
{
    "anycast": {
        "store_and_forward": false,
        "multicast": false
    },
    "broadcast": {
        "store_and_forward": false,
        "multicast": true
    },
    "mytopic": {
        "store_and_forward": true,
        "multicast": true,
        "flavor": "vanilla-topic"
    },
    "myqueue": {
        "store_and_forward": true,
        "multicast": false,
        "flavor": "vanilla-queue"
    }
}
```

Each address that set store-and-forward=true must also refer to a flavor. See below on how to create
your own flavors. To deploy this configuration, you must currently use a barebone client like curl:

    curl -X PUT -H "content-type: application/json" --data-binary @addresses.json http://$(oc get service -o jsonpath='{.spec.clusterIP}' restapi):8080/v1/enmasse/addresses

This will connect to the EnMasse REST API to deploy the address config.

### Sending and receiving messages

For sending and receiving messages, have a look at an example python [sender](http://qpid.apache.org/releases/qpid-proton-0.15.0/proton/python/examples/simple_send.py.html) and [receiver](http://qpid.apache.org/releases/qpid-proton-0.15.0/proton/python/examples/simple_recv.py.html).

To send and receive messages, you can use the local service IP with the clients:

    ./simple_recv.py -a "amqp://$(oc get service -o jsonpath='{.spec.clusterIP}' messaging)/anycast" -m 10

This will block until it has received 10 messages. To start the sender:

    ./simple_send.py -a "amqp://$(oc get service -o jsonpath='{.spec.clusterIP}' messaging)/anycast" -m 10

You can use the client with the 'myqueue' and 'multicast' addresses as well. Making the clients work
with topics is left as an exercies to the reader.

### Flavor config

To support different configurations of brokers, EnMasse comes with different templates that allows
for different broker configurations and broker types.  A flavor is a specific set of parameters for a template. This
allows a cluster administrator to control which configuration settings that are available to the
developer. The flavor configuration is stored in a ConfigMap in OpenShift.

The flavor map can be changed using the `oc` tool:

    oc get configmap flavor -o yaml > flavor.yaml
    # ADD/REMOVE/CHANGE flavors
    oc replace -f flavor.yaml

## Conclusion

We have seen how to setup a messaging service locally, and how to communicate with it using python
example AMQP clients. For further documentation on EnMasse, see the [configuration repository](https://github.com/EnMasseProject/openshift-configuration).
