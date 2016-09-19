{
  template(multicast)::
  {
    local templateName = if multicast then "broadcast" else "anycast",
    "apiVersion": "v1",
    "kind": "Template",
    "metadata": {
      "name": templateName,
    },
    "objects": [
      {
        "apiVersion": "v1",
        "kind": "ConfigMap",
        "metadata": {
          "name": "config-${ADDRESS}",
          "labels": {
            "type": "address-config"
          }
        },
        "data": {
          "address": "${ADDRESS}",
          "store-and-forward": "false",
          "multicast": if multicast then "true" else "false"
        }
      }
    ],
    "parameters": [
      {
        "name": "ADDRESS",
        "description": "The address to use for the address",
        "required": true
      }
    ]
  }
}
