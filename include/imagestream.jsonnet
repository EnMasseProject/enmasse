local version = std.extVar("VERSION");
{
  create(name, repo)::
  {
    "apiVersion": "v1",
    "kind": "ImageStream",
    "metadata": {
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
  }
}
