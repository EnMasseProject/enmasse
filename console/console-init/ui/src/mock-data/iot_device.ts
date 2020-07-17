/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
/**
 * TODO: delete this folder/file after integrate with mock server
 */
const mock_iot_device = {
  via: [
    "device-1",
    "device-2",
    "device-3",
    "device-4",
    "device-5",
    "device-6",
    "device-7",
    "device-8",
    "device-9",
    "device-10"
  ],
  default: {
    "content-type-1": "text/plain",
    "content-type-2": "text/plain",
    "content-type-3": "text/plain",
    long: 12.3544
  },
  ext: {
    custom: {
      level: 0,
      serial_id: "0000",
      location: {
        long: 1.234,
        lat: 5.678
      },
      features: ["foo", "bar", "baz"]
    }
  },
  credentials: [
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
  ]
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

const mock_adapters: any = [
  { type: "http", value: httpAdapter },
  { type: "mqtt", value: mqttAdapter },
  { type: "amqp", value: amqpAdapter },
  { type: "coap", value: coapAdapter }
];

export { mock_iot_device, mock_adapters };
