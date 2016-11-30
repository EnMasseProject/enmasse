# Getting started with EnMasse

This guide will walk through the process of setting up a TLS-enabled EnMasse on OpenShift together
with clients for sending and receiving messages.

## Preqrequisites

In this guide, you need the OpenShift client tools, an OpenShift server, and a recent build of Qpid
Proton for the clients.

### Setting up OpenShift

First, you must download the OpenShift client. You can download [OpenShift
Origin](https://github.com/openshift/origin/releases). EnMasse will work with the latest stable release. 

If you do not have an OpenShift instance running, follow [this guide](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md) for
setting up a local developer instance.

You can also sign up for a [developer preview](https://www.openshift.com/devpreview/) of OpenShift
Online.

### Building/install qpid-proton

Client examples in this guide assume that [Qpid Proton](https://qpid.apache.org/proton/index.html)
python bindings are installed. The clients require being able to specify SNI to work, so until a
version of proton is released with this support, you have to build it from source using the latest
code from the [github repo](https://github.com/apache/qpid-proton). Follow the the [install
instructions](https://git-wip-us.apache.org/repos/asf?p=qpid-proton.git;a=blob_plain;f=INSTALL.md;hb=0.15.0)
for building and installing from source.

## Setting up EnMasse

### Creating project and importing template

EnMasse comes with a few templates that makes setting it up easy. First, create a new project:

    oc new-project enmasse

Then download a script for deploying the EnMasse template:

    curl https://raw.githubusercontent.com/EnMasseProject/openshift-configuration/master/scripts/enmasse-deploy.sh -o enmasse-deploy.sh

This script simplifies the process of deploying the enmasse cluster to your openshift instance. You
can invoke it with `-h` to get a list of options.


### Creating certificates 

Since the service requires TLS, we need to install certificates to be used by the routers in the
messaging service. If you do not have any signed certificates to use, you can generate one with
[openssl](https://www.openssl.org/):

    openssl req -new -x509 -batch -nodes -out server-cert.pem -keyout server-key.pem

### Creating the EnMasse instance

Now you are ready for creating the messaging service itself:

    bash enmasse-deploy.sh -c "https://localhost:8443" -p enmasse -k server-key.pem -s server-cert.pem

This will create the deployments required for running EnMasse. Starting up EnMasse will take a while,
usually depending on how fast it is able to download the docker images for the various components.
In the meantime, you can start to create your address configuration.

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

OpenShift by default only allows HTTP for non-encrypted connections. With TLS, however, we can use
SNI (Server Name Indication) to communicate with the messaging service.

For sending and receiving messages, have a look at an example python [sender](tls_simple_send.py) and [receiver](tls_simple_recv.py).

For SNI, use the host listed by running ```oc get route messaging```. To start the receiver:

    ./tls_simple_recv.py -c amqps://localhost:443 -a anycast -s "$(oc get route -o jsonpath='{.spec.host}' messaging)" -m 10

This will block until it has received 10 messages. To start the sender:

    ./tls_simple_send.py -c amqps://localhost:443 -a anycast -s "$(oc get route -o jsonpath='{.spec.host}' messaging)" -m 10

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

We have seen how to setup a secured messaging service, and how to communicate with it using python
example AMQP clients. For further documentation on EnMasse, see the [configuration repository](https://travis-ci.org/EnMasseProject/openshift-configuration).
