{
    generate(hostname)::
    {
        "kind": "Route",
        "apiVersion": "v1",
        "metadata": {
            "labels": {
              "app": "enmasse"
            },
            "name": "restapi"
        },
        "spec": {
            "path": "/v3/address",
            "host": hostname,
            "to": {
                "kind": "Service",
                "name": "address-controller"
            },
            "port": {
                "targetPort": "http"
            }
        }
    }
}
