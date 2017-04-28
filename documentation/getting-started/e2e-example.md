# Getting started with EnMasse and TLS

This guide will walk through the process of setting up a TLS-enabled EnMasse on OpenShift together
with clients for sending and receiving messages.

## Prerequisites

In this guide, you need the OpenShift client tools, an OpenShift server, and a recent version of Qpid
Proton for the clients.

### Setting up OpenShift

First, you must download the OpenShift client. You can download [OpenShift
Origin](https://github.com/openshift/origin/releases). EnMasse will work with the latest stable release.

If you do not have an OpenShift instance running, follow [this guide](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md) for
setting up a local developer instance.

You can also sign up for a [developer preview](https://www.openshift.com/devpreview/) of OpenShift
Online.

### Installing clients

AMQP client examples in this guide assume that [Qpid Proton](https://qpid.apache.org/proton/index.html) with python bindings are installed ( > 0.16.0). The python bindings are also available in [PyPI](https://pypi.python.org/pypi/python-qpid-proton/0.16.0).

MQTT client examples use [Eclipse Paho Python library ](https://github.com/eclipse/paho.mqtt.python). It needs to be installed from the source code, following the instruction on the GitHub site because the TLS needed
features aren't in the latest release yet. Note that this features are in the "develop" branch and not in the "master" one.

## Setting up EnMasse

### Creating certificates

Since the service requires TLS, we need to install certificates to be used by the routers in the
messaging service. If you do not have any signed certificates to use, you can generate one with
[openssl](https://www.openssl.org/):

    openssl req -new -x509 -batch -nodes -out server-cert.pem -keyout server-key.pem

### Creating project and importing template

EnMasse comes with a few templates that makes setting it up easy. First, create a new project:

    oc new-project enmasse

You can setup EnMasse automatically or manually. 

#### Deploying EnMasse automatically

To create it automatically, download the deployment script:

    curl -o enmasse-deploy.sh https://raw.githubusercontent.com/EnMasseProject/enmasse/master/scripts/enmasse-deploy.sh

This script simplifies the process of deploying the enmasse cluster to your openshift instance. You
can invoke it with `-h` to get a list of options. To deploy:

    bash enmasse-deploy.sh -c "https://localhost:8443" -p enmasse -k server-key.pem -s server-cert.pem

This will create the deployments required for running EnMasse. Starting up EnMasse will take a while,
usually depending on how fast it is able to download the docker images for the various components.
In the meantime, you can start to create your address configuration.


#### Deploying EnMasse manually

Login as developer:

    oc login https://localhost:8443 -u developer

Create new project enmasse:

    oc new-project enmasse

Create service account for address controller:

    oc create sa enmasse-service-account -n enmasse

Add permissions for viewing OpenShift resources to default user:

    oc policy add-role-to-user view system:serviceaccount:enmasse:default

Add permissions for editing OpenShift resources to EnMasse service account:

    oc policy add-role-to-user edit system:serviceaccount:enmasse:enmasse-service-account

Create certificate secret for router:

    oc secret new qdrouterd-certs server-cert.pem server-key.pem

Add router secret to default service account:

    oc secret add serviceaccount/default secrets/qdrouterd-certs --for=mount

Create certificate secret for MQTT gateway:

    oc secret new mqtt-certs server-cert.pem server-key.pem

Add MQTT secret to default service account:

    oc secret add serviceaccount/default secrets/mqtt-certs --for=mount

Instantiate EnMasse template:

    oc process -f https://raw.githubusercontent.com/EnMasseProject/enmasse/master/generated/tls-enmasse-template.yaml  | oc create -n enmasse -f -

### Configuring addresses

EnMasse is configured with a set of addresses that you can use for messages. Currently, EnMasse supports 4 different address types:

   * Brokered queues
   * Brokered topics (pub/sub)
   * Direct anycast addresses
   * Direct broadcast addresses

#### Configuring addresses using the console

The EnMasse console should be available at `http://$(minikube ip)/console`. You can create and
monitor queues and topics using the UI.

#### Configuring addresses using the REST api

Here is an example config with all 4 variants that you can save to `addresses.json`:

```
{
    "apiVersion": "v3",
    "kind": "AddressList",
    "items": [
        {
            "metadata": {
                "name": "anycast"
            },
            "spec": {
                "store_and_forward": false,
                "multicast": false
            }
        },
        {
            "metadata": {
                "name": "broadcast"
            },
            "spec": {
                "store_and_forward": false,
                "multicast": true 
            }
        },
        {
            "metadata": {
                "name": "myqueue"
            },
            "spec": {
                "store_and_forward": true,
                "multicast": false,
                "flavor": "vanilla-queue"
            }
        },
        {
            "metadata": {
                "name": "mytopic"
            },
            "spec": {
                "store_and_forward": true,
                "multicast": true,
                "flavor": "vanilla-topic"
            }
        }
    ]
}
```

Each address that set store-and-forward=true must also refer to a flavor. See below on how to create
your own flavors. To deploy this configuration, you must currently use a barebone client like curl:

    curl -X PUT -H "content-type: application/json" --data-binary @addresses.json http://$(oc get route -o jsonpath='{.spec.host}' restapi)/v3/address

This will connect to the EnMasse REST API to deploy the address config.

### Sending and receiving messages

#### AMQP

OpenShift by default only allows HTTP for non-encrypted connections. With TLS, however, we can use
SNI (Server Name Indication) to communicate with the messaging service.

For sending and receiving messages, have a look at an example python [sender](tls_simple_send.py) and [receiver](tls_simple_recv.py).

For SNI, use the host listed by running ```oc get route messaging```. To start the receiver:

    ./tls_simple_recv.py -c amqps://localhost:443 -a anycast -s "$(oc get route -o jsonpath='{.spec.host}' messaging)" -m 10

This will block until it has received 10 messages. To start the sender:

    ./tls_simple_send.py -c amqps://localhost:443 -a anycast -s "$(oc get route -o jsonpath='{.spec.host}' messaging)" -m 10

You can use the client with the 'myqueue' and 'broadcast' and 'mytopic' addresses as well.

#### MQTT

OpenShift allows connections with TLS using the SNI (Server Name Indication) to communicate with the MQTT service. If you want to run
EnMasse locally, it could be needed to apply a workaround for having connection hostname equals to the SNI name; you could modify the
`/etc/hosts` file in order to map the localhost address to the route hostname.

For sending and receiving messages, have a look at an example python [sender](tls_mqtt_send.py) and [receiver](tls_mqtt_recv.py).

In order to subscribe to a topic (i.e. `mytopic` from the previous addresses configuration), the receiver client can be used in the following way :

    ./tls_mqtt_recv.py -c "$(oc get route -o jsonpath='{.spec.host}' mqtt)" -p 443 -t mytopic -q 1 -s ./server-cert.pem

Then the subscriber is waiting for messages published on that topic. To start the publisher, the sender client can be used in the following way :

    ./tls_mqtt_send.py -c "$(oc get route -o jsonpath='{.spec.host}' mqtt)" -p 443 -t mytopic -q 1 -s ./server-cert.pem -m "Hello EnMasse"

The the publisher publishes the message and disconnects from EnMasse. The message is received by the previous connected subscriber.

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
example AMQP clients.
