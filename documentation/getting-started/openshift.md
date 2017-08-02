# EnMasse on OpenShift

This guide will walk through the process of setting up EnMasse on OpenShift with clients for sending and receiving messages.

## Preqrequisites

In this guide, you need the OpenShift client tools.  You can download the [OpenShift Origin](https://github.com/openshift/origin/releases) client for this guide. EnMasse will work with the latest stable release. 

If you have an OpenShift instance running already, you can start setting up EnMasse. If not, follow [this guide](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md) for setting up a local developer instance of OpenShift.

## Setting up EnMasse

### Installing

Download one of the releases from https://github.com/EnMasseProject/enmasse/releases and unpack it.
Once unpacked, you can either deploy EnMasse using an automated script or follow the below steps.

#### Deploying EnMasse automatically

The deployment script simplifies the process of deploying the enmasse cluster. You
can invoke it with `-h` to get a list of options. To deploy:

    ./deploy-openshift.sh -m "https://localhost:8443" -n enmasse

This will create the deployments required for running EnMasse. Starting up EnMasse will take a while,
usually depending on how fast it is able to download the docker images for the various components.
In the meantime, you can start to create your address configuration.

#### Deploying EnMasse manually

Login as developer:

    oc login -u developer  https://localhost:8443

Create new project enmasse:

    oc new-project enmasse

Create service account for address controller:

    oc create sa enmasse-service-account -n enmasse

Add permissions for viewing OpenShift resources to default user:

    oc policy add-role-to-user view system:serviceaccount:enmasse:default

Add permissions for editing OpenShift resources to EnMasse service account:

    oc policy add-role-to-user edit system:serviceaccount:enmasse:enmasse-service-account

Create signer CA:

    oc adm ca create-signer-cert --key=ca.key --cert=ca.crt --serial=ca.serial.txt --name=enmasse-signer@$(date +%s)

Create signed server certificate for address-controller:

    oc adm ca create-server-cert --key=enmasse-controller-pkcs1.key --cert=enmasse-controller.crt --hostnames=address-controller.enmasse.svc.cluster.local --signer-cert=ca.crt --signer-key=ca.key --signer-serial=ca.serial.txt

Convert key to correct PKCS#8 format:

    openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in enmasse-controller-pkcs1.key -out enmasse-controller.key

Create secret for controller certificate:

    oc secret new enmasse-controller-certs tls.crt=enmasse-controller.crt tls.key=enmasse-controller.key

Add controller secret mount permissions for enmasse-service-account:

    oc secret add serviceaccount/enmasse-service-account secrets/enmasse-controller-certs --for=mount

Instantiate EnMasse template:

    oc process -f openshift/enmasse.yaml  | oc create -n enmasse -f -


#### Deploying EnMasse with authentication enabled

The initial support for authentication relies on a user database that
the messaging service uses to authenticate against. This requires a
persistent volume on which the user database exists, that is then
mounted into the necessary pods.

To create a local host based persistent volume for development or
evaluation purposes, you must first login as the admin user:

    oc login https://localhost:8443 -u system:admin

Then create a directory, with read and write permissions for all, that
will be used for the persistent volume. E.g.

    mkdir /tmp/sasldb && chmod a+x /tmp/sasldb

Then create the peristent volume, e.g. using the example yaml
https://github.com/EnMasseProject/enmasse/tree/master/templates/include/sasldb-persistent-volume.yaml:

    oc create -f https://raw.githubusercontent.com/EnMasseProject/enmasse/master/templates/include/sasldb-persistent-volume.yaml

Then log in again as developer:

    oc login https://localhost:8443 -u developer

Then follow the instructions for manual deployment above, substituting
enmasse-with-sasldb.yaml for enmasse.yaml in the last
step.

The users can be managed through the console. Note that when
authenticating against the messaging service you need to specify the
domain which is 'enmasse', e.g. myuser@enmasse.

[Note also that anonymous is still enabled on the routers, until all
internal services have been updated to authenticate when connecting to
the messaging service.]

### Configuring addresses

EnMasse is configured with a set of addresses that you can use for messages. Currently, EnMasse supports 4 different address types:

   * Brokered queues
   * Brokered topics (pub/sub)
   * Direct anycast addresses
   * Direct broadcast addresses

See the [address model](../address-model/model.md) for details. EnMasse also comes with a console that you can use for managing addresses. You can get the console URL by running
    
    echo "http://$(oc get route -o jsonpath='{.spec.host}' console)"

You can also deploy the addressing config using the address controller API. See [resource definitions](../address-model/resource-definitions.md) for details on the resources consumed by the API.  Here is an example config with all 4 variants that you can save to `addresses.json`:

```
{
  "apiVersion": "enmasse.io/v1",
  "kind": "AddressList",
  "items": [
    {
      "metadata": {
        "name": "myqueue"
      },
      "spec": {
        "type": "queue"
      }
    },
    {
      "metadata": {
        "name": "mytopic"
      },
      "spec": {
        "type": "topic"
      }
    },
    {
      "metadata": {
        "name": "myanycast"
      },
      "spec": {
        "type": "anycast"
      }
    },
    {
      "metadata": {
        "name": "mymulticast"
      },
      "spec": {
        "type": "multicast"
      }
    }
  ]
}
```

Each address that set store-and-forward=true must also refer to a flavor. See below on how to create
your own flavors. To deploy this configuration, you must currently use a http client like curl:

    curl -X POST -H "content-type: application/json" --data-binary @addresses.json http://$(oc get route -o jsonpath='{.spec.host}' restapi)/v1/addresses/default

This will connect to the address controller REST API to deploy the address config.

### Sending and receiving messages

#### AMQP

For sending and receiving messages, have a look at an example python [sender](http://qpid.apache.org/releases/qpid-proton-0.15.0/proton/python/examples/simple_send.py.html) and [receiver](http://qpid.apache.org/releases/qpid-proton-0.15.0/proton/python/examples/simple_recv.py.html).

To send and receive messages, you can either connect using the local service IP or the external
route. To connect a client using the local service IP:

    ./simple_recv.py -a "amqp://$(oc get service -o jsonpath='{.spec.clusterIP}' messaging)/anycast" -m 10

This will block until it has received 10 messages. To start the sender:

    ./simple_send.py -a "amqp://$(oc get service -o jsonpath='{.spec.clusterIP}' messaging)/anycast" -m 10

You can use the client with the 'myqueue' and 'broadcast' and 'mytopic' addresses as well.

To use the external routes for sending and receiving messages:

    ./simple_send.py -a "amqps://$(oc get route -o jsonpath='{.spec.host}' messaging):443/anycast" -m 10

#### MQTT

For sending and receiving messages, the `quitto_sub -h $(oc get service -o
jsonpath='{.spec.clusterIP}' mqtt) -t mytopic -q 1osquitto` clients are the simpler way to go. These clients
can be used either against the local service IP or the external route. To connect using the local
service IP:

In order to subscribe to a topic (i.e. `mytopic` from the previous addresses configuration), the `mosquitto_sub` can be used in the following way :

    mosquitto_sub -h $(oc get service -o jsonpath='{.spec.clusterIP}' mqtt) -t mytopic -q 1

Then the subscriber is waiting for messages published on that topic. To start the publisher, the `mosquitto_pub` can be used in the following way :

    mosquitto_pub -h $(oc get service -o jsonpath='{.spec.clusterIP}' mqtt) -t mytopic -q 1 -m "Hello EnMasse"

The the publisher publishes the message and disconnects from EnMasse. The message is received by the
previous connected subscriber. 

For sending and receiving messages using the external route, have a look at an example python [sender](tls_mqtt_send.py) and [receiver](tls_mqtt_recv.py).

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

We have seen how to setup EnMasse, and how to communicate with it using AMQP and MQTT clients.
