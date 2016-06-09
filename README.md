# Setting up MaaS

## Setting up configuration service

Add view policy to the default serviceaccount:

    oc policy add-role-to-user view system:serviceaccount:myproject:default

Start the configuration service endpoint and the replication controller:

    oc create -f configuration-service.yaml
    oc create -f config-subscription-rc.yaml

The configuration service will watch all configmaps for the project deployed to.

### Creating config map

Create initial config map:

    oc create -f addresses.yaml

Where address.yaml is of the form:

```
kind: ConfigMap
apiVersion: v1
metadata:
  name: maas
data:
  json: |
    {
        "queue1": { "store_and_forward":true, "multicast":false },
        "queue2": { "store_and_forward":true, "multicast":false },
        "anycast": { "store_and_forward":false, "multicast":false },
        "broadcast": { "store_and_forward":false, "multicast":true }
    }
```

After editing that file, update the config map with:

    oc replace -f addresses.yaml

## Setting up config generator agent

In order for the agent to generate broker replication controllers, it must be granted edit rights:

    oc policy add-role-to-user edit system:serviceaccount:myproject:deployer

Then start the replication controller:

    oc create -f config-generator-rc.yaml

Whenever you change the 'maas' config map, the config-generator-agent will pick it up and
delete/create/update broker clusters.

## Setting up the routers

Start the router agent service replication controller:

    oc create -f ragent-service.yaml
    oc create -f ragent-rc.yaml

The router agent(s) update the routers to match the desired addresses
and to maintain full-mesh connectivity.

Start the router service and replication-controller:

    oc create -f qdrouterd-service.yaml
    oc create -f qdrouterd-rc.yaml

You can get the service IP either from the web console or by running:

    oc get service qdrouterd -o yaml

You should now be able to connect to that to send and receive
messages based on the addressing scheme in the config map.