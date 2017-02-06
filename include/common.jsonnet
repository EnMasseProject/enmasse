local version = std.extVar("VERSION");
{
  container(name, image, port_name, port, mem_request)::
  {
    "image": image + ":" + version,
    "name": name,
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

  containerWithEnv(name, image, port_name, port, env, mem_request)::
  {
    "image": image + ":" + version,
    "name": name,
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
    "env": env,
    "livenessProbe": {
      "tcpSocket": {
        "port": port_name
      }
    }
  },

  container2(name, image, port_name, port, port2_name, port2_port, mem_request)::
  {
    "image": image + ":" + version,
    "name": name,
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
      },
      {
        "name": port2_name,
        "containerPort": port2_port,
        "protocol": "TCP"
      }
    ],
    "livenessProbe": {
      "tcpSocket": {
        "port": port_name
      }
    }
  },

  service(name, selector_name, port_name, port, target_port)::
  {
    "apiVersion": "v1",
    "kind": "Service",
    "metadata": {
      "name": name,
      "labels": {
        "app": "enmasse"
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
  }
}
