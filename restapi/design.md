This page documents the REST API of EnMasse

# Overview

The REST API is intended to be the user facing API for managing EnMasse. There is also undocumented support for pushing configuration using AMQP if desired. This page will document the HTTP API


# API 

| Method | Path | Description | Example request body | Example response body |
| GET | /v1/enmasse/addresses/ | Returns the map of addresses | N/A | `{ "myqueue": { "store_and_forward": true, "multicast": false, "flavor": "vanilla-queue"}, "anycast": {"store_and_forward": false, "multicast": false}}` |
| PUT | /v1/enmasse/addresses/ | Replaces the map of addresses | `{"mytopic": {"store_and_forward": true, "multicast": true, "flavor": "vanilla-topic"}}` | `{"mytopic": {"store_and_forward": true, "multicast": true, "flavor": "vanilla-topic"}}`  |
| POST | /v1/enmasse/addresses/ | Appends the map of addresses to the existing | `{"mytopic": {"store_and_forward": true, "multicast": true, "flavor": "vanilla-topic"}}` | `"myqueue": { "store_and_forward": true, "multicast": false, "flavor": "vanilla-queue"}, "anycast": {"store_and_forward": false, "multicast": false}, {"mytopic": {"store_and_forward": true, "multicast": true, "flavor": "vanilla-topic"}}`  |
| DELETE | /v1/enmasse/addresses/ | Delete the provided addresses | `["myqueue"]` | `{"anycast": {"store_and_forward": false, "multicast": false}}` |
