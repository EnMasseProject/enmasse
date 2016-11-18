# Getting started with EnMasse

This guide will walk through the process of setting up a TLS-enabled EnMasse on OpenShift together
with clients for sending and receiving messages.

## Setting up OpenShift (optional)

First, you must download the OpenShift client. You can download [OpenShift
Origin](https://github.com/openshift/origin/releases). EnMasse will work with the latest stable release. 

If you do not have an instance of OpenShift available, follow [this guide](https://github.com/openshift/origin/blob/master/docs/cluster_up_down.md) for
setting up a local developer instance.

You can also sign up for a [developer preview](https://www.openshift.com/devpreview/) of OpenShift
Online.

## Setting up EnMasse

### Creating project and importing template

EnMasse comes with a few templates that makes setting it up easy. First, create a new project:

    oc new-project enmasse

Then import the EnMasse template:

    oc create -f https://raw.githubusercontent.com/EnMasseProject/openshift-configuration/master/generated/tls-enmasse-template.yaml

### Setting up permissions 

At the moment of writing, some permissions need to be granted before creating the service. View permission should
be granted to the default serviceaccount. Edit rights must also be granted to the deployer role:

    oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default
    oc policy add-role-to-user edit system:serviceaccount:$(oc project -q):deployer


### Setting up certificates 

Since the service requires TLS, we need to install certificates to be used by the routers in the
messaging service. If you do not have any signed certificates to use, you can generate one with
[openssl](https://www.openssl.org/):

    openssl req -new -x509 -batch -nodes -out server-cert.pem -keyout server-key.pem

To install the certificates for EnMasse, a secret must be added to OpenShift:

    oc secret new qdrouterd-certs server-cert.pem server-key.pem

Then, the default serviceaccount must be allowed to read this secret:

    oc secret add serviceaccount/default secrets/qdrouterd-certs --for=mount

### Creating the EnMasse instance

Now you are ready for creating the messaging service itself:

    oc new-app --template=tls-enmasse -l app=enmasse

By default, the template will setup 4 addresses with 4 different address types:

   * A queue named 'myqueue'
   * A topic named 'mytopic'
   * A 'direct' address 'anycast'
   * A 'direct' address 'multicast'

### Sending and receiving messages

OpenShift by default only allows HTTP for non-encrypted connections. With TLS, however, we can use
SNI (Server Name Indication) to communicate with the messaging service.

For sending and receiving messages, have a look at an example python [sender](tls_simple_send.py) and [receiver](tls_simple_recv.py).

For SNI, use the host listed by running ```oc get route messaging```. To start the receiver:

    $ ./tls_simple_recv.py -c amqps://localhost:443 -a anycast -s enmasse-messaging-service.192.168.1.6.xip.io -m 10

This will block until it has received 10 messages. To start the sender:

    $ ./tls_simple_send.py -c amqps://localhost:443 -a anycast -s enmasse-messaging-service.192.168.1.6.xip.io -m 10

You can use the client with the 'myqueue' and 'multicast' addresses as well. Making the clients work
with topics is left as an exercies to the reader.

### Address configuration

The addresses are defined in a config map called 'maas'. To make a change to the configuration,
download the config, edit it, and replace it:

    oc get configmap maas -o yaml > addresses.yaml

    # ADD/REMOVE/EDIT addresses 

    oc replace -f addresses.yaml

The changes will be picked up by the storage controller, which will create and delete brokers to
match the desired state.

Each address that set store-and-forward=true must also refer to a flavor.

### Flavor config

To support different configurations of brokers, EnMasse comes with different templates that allows
for different broker configurations and broker types.  A flavor is a specific set of parameters for a template. This
allows a cluster administrator to control which configuration settings that are available to the developer.

The flavor map can be changed in a similar fashion to the address config:

   oc get configmap flavor -o yaml > flavor.yaml
   # ADD/REMOVE/CHANGE flavors
   oc replace -f flavor.yaml

## Conclusion

We have seen how to setup a secured messaging service, and how to communicate with it using python
example AMQP clients. For further documentation on EnMasse, see the [configuration repository](https://travis-ci.org/EnMasseProject/openshift-configuration).
