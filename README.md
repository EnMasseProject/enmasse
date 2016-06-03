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

    curl -k "https://localhost:8443/api/v1/namespaces/myproject/configmaps" -H "Authorization: Bearer <token>" -X POST -d @mymap.json  -H "Content-Type: application/json"

The contents of mymap.json:

```
{
  "kind": "ConfigMap",
  "apiVersion": "v1",
  "metadata": {
    "name": "maas"
  },
  "data": {
    "json": "{\"myaddr\":{\"store-and-forward\":true,\"multicast\":false}}"
  }
}
```

To update the map, modify the mymap.json and do a PUT on the myprojects/configmaps/maas resource.

Note: you can also create the config map like this:

    oc create -f mymap.json

and after editing mymap.json:

    oc replace -f mymap.json

If you have a json file with the addresses you can create the config map using:

   oc create configmap maas --from-file=json=addresses.json

The config map can also be specified in yaml, e.g. with mypamp.yaml containing:

```
kind: ConfigMap
apiVersion: v1
metadata:
  name: maas
data:
  json: |
    {
        "myaddr": { "store_and_forward":true, "multicast":false }
    }
```

run:

    oc create -f mymap.yaml

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
