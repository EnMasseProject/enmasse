# Setting up MaaS

## Setting up configuration service

Add view policy to the default serviceaccount:

    oc policy add-role-to-user view system:serviceaccount:myproject:default

Create initial config map (the openshift-restclient-java will crash if trying to watch when there
are no config maps):

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

To update the map, modify the mymap.json and do a PUT on the myprojects/configmaps/maas resource. Start the configuration service endpoint and the replication controller:

    oc create -f configuration-service.yaml
    oc create -f config-subscription-rc.yaml

## Setting up config generator agent

In order for the agent to create new replication controllers, it must be granted edit rights:

    oc policy add-role-to-user edit system:serviceaccount:myproject:deployer 

Then start the replication controller:

    oc create -f config-generator-rc.yaml
