{
  container(name, image, port_name, port, mem_request, env)::
  {
    "image": image,
    "name": name,
    "env": env,
    "resources": {
        "requests": {
            "memory": mem_request,
        },
        "limits": {
            "memory": mem_request,
        }
    },
    "ports": [
      {
        "name": port_name,
        "containerPort": port,
        "protocol": "TCP"
      }
    ],
    "livenessProbe": {
      "tcpSocket": {
        "port": port_name
      }
    }
  },

  clientContainer(name, image, mem_request, env, volumeMounts, http_health, http_ready)::
  {
    local health_port =
    {
      "name": "health",
      "containerPort": 8080
    },
    local health_probe =
    {
      "httpGet": {
        "path": "/health",
        "port": "health"
      }
    },
    local ready_port =
    {
      "name": "ready",
      "containerPort": 8080
    },
    local ready_probe =
    {
      "httpGet": {
        "path": "/ready",
        "port": "ready"
      }
    },
    "image": image,
    "name": name,
    "env": env,
    "volumeMounts": volumeMounts,
    "resources": {
        "requests": {
            "memory": mem_request,
        },
        "limits": {
            "memory": mem_request,
        }
    },
    [if http_health && !http_ready then "ports"]: [ health_port ],
    [if !http_health && http_ready then "ports"]: [ ready_port ],
    [if http_health && http_ready then "ports"]: [ health_port, ready_port ],
    [if http_health then "livenessProbe"]: health_probe,
    [if http_ready then "readinessProbe"]: ready_probe
  },

  service(addressSpace, name, selector_name, port_name, port, target_port)::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": name,
      "labels": {
        "app": "enmasse"
      },
      "annotations": {
        "addressSpace": addressSpace
      }
    },
    "spec": {
      "ports": [
        {
          "name": port_name,
          "port": port,
          "protocol": "TCP",
          "targetPort": target_port
        }
      ],
      "selector": {
        "name": selector_name
      }
    }
  },

  empty_secret(name)::
  {
    "apiVersion": "v1",
    "kind": "Secret",
    "metadata": {
      "name": name
    },
    "data": {}
  },

  ca_secret(name, cert_value)::
  {
    "apiVersion": "v1",
    "kind": "Secret",
    "metadata": {
      "name": name
    },
    "data": {
      "tls.crt": cert_value
    }
  }
}
