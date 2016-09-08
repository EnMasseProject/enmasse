{
  container(addressEnv)::
    {
      "name": "forwarder",
      "env": [ addressEnv ],
      "image": "${TOPIC_FORWARDER_IMAGE}"
    }
}
