# Setting up EnMasse

# Preqrequisites

   * Openshift version >= 1.3

### Note for contributors

To make changes, edit the jsonnet files in root and include/ folders. To generate the templates, run
'make'. 

# Quick start

## Optional: Setup openshift test cluster

If you don't have an openshift cluster available, you can easily set one up locally by downloading
the openshift client from https://github.com/openshift/origin/releases and run 'oc cluster up'.

## Access permissions

Some permissions need to be granted before setting up the messaging
service.  View permission should be granted to the default
serviceaccount (system:serviceaccount:myproject:default for a project
named myproject). This is needed by the configmap-bridge and the
router agent.

Edit rights must also be granted to the deployer role, used by the
storage-controller.

The permissions can be setup with the following commands:

    oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default
    oc policy add-role-to-user edit system:serviceaccount:$(oc project -q):deployer

## Using the simple template

There is a template that can be used to setup the necessary objects in
openshift. To use it, run:

    oc process -f https://raw.githubusercontent.com/EnMasseProject/openshift-configuration/master/generated/enmasse-template.yaml | oc create -f -

## Using the TLS template

For the messaging service to be externally accessible, a route must be
setup over TLS. This requires qdrouterd containers to have TLS enabled.

To do this we need to create a secret called qdrouterd-certs with the
key and certificate for the routers contained in server-key.pem and
server-cert.pem respectively:

    oc secret new qdrouterd-certs server-cert.pem server-key.pem

Then the default serviceaccount (under which the router containers are
currently run) needs to be allowed to mount this secret:

    oc secret add serviceaccount/default secrets/qdrouterd-certs --for=mount

Then we can use the tls version of the template as follows:

    oc process -f https://raw.githubusercontent.com/EnMasseProject/openshift-configuration/master/generated/tls-enmasse-template.yaml | oc create -f -

This will also create a route called 'messaging' using TLS passthrough. In order for this to work,
you must ensure that your OpenShift cluster has a
[router](https://docs.openshift.org/latest/install_config/router/index.html#install-config-router-overview). Then, you can connect to openshift cluster using AMQP over TLS with SNI. Have a look at the [rhea TLS example](https://github.com/grs/rhea/blob/master/examples/tls/tls_client.js). The SNI host would be the host generated for the 'messaging' route.

## Configuring

By default, the template will setup 4 addresses with the following 4 different address types:

   * Queue
   * Topic
   * 'Direct' anycast
   * 'Direct' multicast

The addresses are defined in a config map called 'maas'. To make a change to the configuration,
download the config, edit it, and replace it:

   oc get configmap maas -o yaml > addresses.yaml
   # ADD/REMOVE/EDIT addresses
   oc replace -f addresses.yaml

The changes will be picked up by the storage controller, which will create and delete brokers to
match the desired state.

Each address that set store-and-forward=true must also refer to a flavor.

### Flavor config

To support different configurations of brokers, EnMasse comes with different templates that allow
you to setup persistence and TLS. A flavor is a specific set of parameters for a template. This
allows a cluster administrator to control which configuration settings that are available to the
developer.

The flavor map can be changed in a similar fashion to the address config:

   oc get configmap flavor -o yaml > flavor.yaml
   # ADD/REMOVE/CHANGE flavors
   oc replace -f flavor.yaml

# Benchmarking

EnMasse provides a benchmarking suite, ebench, that can run alongside the EnMasse cluster or
on a separate set of machines. The suite is composed of an agent that sends messages to a specific
address and a collector that aggregates metrics from multiple agents. To start an agent and a
collector:

    oc create -f ebench-agent-dc.yaml
    oc create -f ebench-collector.yaml

The agent and collector is parameterized using environment variables defined in the specification
file. 

You can scale the number of agents by adjusting the number of replicas, and the collector will
automatically pick up the changes and display aggregated results for all agents running.

