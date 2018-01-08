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
      },
      "initialDelaySeconds": 60
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
      },
      "initialDelaySeconds": 60
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

  secret_volume(name, secret)::
    {
      "name": name,
      "secret": {
        "secretName": secret
      }
    },

  persistent_volume(name, claimName)::
  {
    "name": name,
    "persistentVolumeClaim": {
        "claimName": claimName
    }
  },

  empty_volume(name)::
  {
    "name": name,
    "emptyDir": {}
  },

  configmap_volume(name, mapName)::
  {
    "name": name,
    "configMap": {
      "name": mapName,
    }
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
  },

  exec_probe(command, initialDelay=0)::
  {
    "exec": {
      "command": command,
    },
    [if initialDelay != 0 then "initialDelaySeconds"]: initialDelay
  },

  tcp_probe(port, initialDelay=0)::
  {
    "tcpSocket": {
      "port": port
    },
    [if initialDelay != 0 then "initialDelaySeconds"]: initialDelay
  },

  http_probe(port, path, scheme, initialDelay=0)::
  {
    "httpGet": {
      "port": port,
      "path": path,
      "scheme": scheme
    },
    [if initialDelay != 0 then "initialDelaySeconds"]: initialDelay
  },

  container_port(name, port)::
  {
    "name": name,
    "containerPort": port
  },

  volume_mount(name, mountPath, readOnly=false)::
  {
    "name": name,
    "mountPath": mountPath,
    "readOnly": readOnly
  },

  env(name, value)::
  {
    "name": name,
    "value": value
  },

  env_field_ref(name, field_path)::
  {
    "name": name,
    "valueFrom": {
      "fieldRef": {
        "fieldPath": field_path
      }
    }
  },

  memory_resources(request, limit)::
  {
    "requests": {
      "memory": request
    },
    "limits": {
      "memory": limit
    }
  }
}
