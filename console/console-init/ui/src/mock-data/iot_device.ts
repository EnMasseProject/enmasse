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

// TODO: Remove mock gateway devices and mock gateway groups once the query to populate add gateway typeahead is ready.
const mockGatewayGroups: any[] = [
  {
    id: "group-1",
    isDisabled: false,
    key: "group-1",
    value: "group-1"
  },
  {
    id: "group-2",
    isDisabled: false,
    key: "group-2",
    value: "group-2"
  },
  {
    id: "group-3",
    isDisabled: false,
    key: "group-3",
    value: "group-3"
  },
  {
    id: "group-4",
    isDisabled: false,
    key: "group-4",
    value: "group-4"
  },
  {
    id: "group-5",
    isDisabled: false,
    key: "group-5",
    value: "group-5"
  },
  {
    id: "group-6",
    isDisabled: false,
    key: "group-6",
    value: "group-6"
  },
  {
    id: "group-7",
    isDisabled: false,
    key: "group-7",
    value: "group-7"
  }
];

const mockGatewayDevices: any[] = [
  {
    id: "device-1",
    isDisabled: false,
    key: "device-1",
    value: "device-1"
  },
  {
    id: "device-2",
    isDisabled: false,
    key: "device-2",
    value: "device-2"
  },
  {
    id: "device-3",
    isDisabled: false,
    key: "device-3",
    value: "device-3"
  },
  {
    id: "device-4",
    isDisabled: false,
    key: "device-4",
    value: "device-4"
  },
  {
    id: "device-5",
    isDisabled: false,
    key: "device-5",
    value: "device-5"
  },
  {
    id: "device-6",
    isDisabled: false,
    key: "device-6",
    value: "device-6"
  },
  {
    id: "device-7",
    isDisabled: false,
    key: "device-7",
    value: "device-7"
  }
];

export {
  mock_iot_device,
  mock_adapters,
  mockGatewayDevices,
  mockGatewayGroups
};
