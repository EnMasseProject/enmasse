{
  container()::
    {
      "name": "forwarder",
      "env": [ { "name": "TOPIC_NAME", "value": "${ADDRESS}" } ],
      "image": "${TOPIC_FORWARDER_IMAGE}"
    }
}
