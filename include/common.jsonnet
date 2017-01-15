local version = std.extVar("VERSION");
{
  imagestream(name, repo)::
  {
    "apiVersion": "v1",
    "kind": "ImageStream",
    "metadata": {
      "labels": {
        "app": "enmasse"
      },
      "name": name
    },
    "spec": {
      "dockerImageRepository": repo, 
      "tags": [
        {
          "name": version,
          "from": {
            "kind": "DockerImage",
            "name": repo + ":" + version
          }
        }
      ],
      "importPolicy": {
        "scheduled": true
      }
    }
  },

  trigger(name, image)::
  {
    "type": "ImageChange",
    "imageChangeParams": {
      "automatic": true,
      "containerNames": [
        name
      ],
      "from": {
        "kind": "ImageStreamTag",
        "name": image + ":" + version
      }
    }
  },

  container(name, image, port_name, port)::
  {
    "image": name,
    "name": name,
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

  container2(name, image, port_name, port, port2_name, port2_port)::
  {
    "image": name,
    "name": name,
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
