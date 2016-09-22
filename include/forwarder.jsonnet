local is = import "imagestream.jsonnet";
{
  imagestream(image_name)::
    is.create("topic-forwarder", image_name),
  container(addressEnv)::
    {
      "name": "forwarder",
      "env": [ addressEnv ],
      "image": "topic-forwarder",
      "ports": [
        {
          "name": "health",
          "containerPort": 8080
        }
      ],
      "livenessProbe": {
        "httpGet": {
          "path": "/health",
          "port": "health"
        }
      }
    }
}
