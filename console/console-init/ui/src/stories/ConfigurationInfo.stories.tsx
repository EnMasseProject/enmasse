/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { ConfigurationInfo } from "modules/iot-device-detail/components";

export default {
  title: "ConfigurationInfo"
};

const httpAdapter = {
  url: "https://http.bosch-iot-hub.com"
};
const mqttAdapter = {
  tls: true,
  host: "mange.bosh-iot-hub.com",
  port: 8883
};
const amqpAdapter = {
  url: "https://http.bosch-iot-hub.com"
};
const coapAdapter = {
  url: "https://http.bosch-iot-hub.com"
};
const adapters: any = [
  { type: "http", value: httpAdapter },
  { type: "mqtt", value: mqttAdapter },
  { type: "amqp", value: amqpAdapter },
  { type: "coap", value: coapAdapter }
];

const credentials = [
  {
    type: "hashed-password",
    "auth-id": "user-1",
    enabled: false,
    secrets: [
      {
        "not-after": "2020-10-01T10:00:00Z",
        "pwd-hash": "bjb232138d"
      },
      {
        "not-before": "2020-10-01T10:00:00Z",
        "pwd-hash": "adfhk327823"
      }
    ]
  },
  {
    type: "hashed-password",
    "auth-id": "alternate-user-1",
    enabled: true,
    secrets: [
      {
        comment: "was just for testing"
      }
    ]
  },
  {
    type: "psk",
    "auth-id": "user-1",
    secrets: [
      {
        key: "123knsd8=",
        comment: "was just for testing"
      }
    ]
  },
  {
    type: "x509-cert",
    "auth-id": "other-id-1",
    enabled: false,
    secrets: [],
    ext: {
      "para-1": "value-1",
      "para-2": "value-2",
      "para-3": "value-3",
      "para-4": "value-4"
    }
  }
];

export const ConfigurationInfoPage = () => (
  <ConfigurationInfo
    id="ci-page"
    credentials={credentials}
    filterType="all"
    filterValue="all"
    setFilterType={() => {}}
    setFilterValue={() => {}}
  />
);
