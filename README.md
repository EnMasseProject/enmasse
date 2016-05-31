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

## Setting up config generator agent

In order for the agent to generate broker replication controllers, it must be granted edit rights:

    oc policy add-role-to-user edit system:serviceaccount:myproject:deployer 

Then start the replication controller:

    oc create -f config-generator-rc.yaml

Whenever you change the 'maas' config map, the config-generator-agent will pick it up and
delete/create/update broker clusters.
