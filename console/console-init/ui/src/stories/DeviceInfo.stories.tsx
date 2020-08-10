/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { HashRouter as Router } from "react-router-dom";
import { DeviceInfo } from "modules/iot-device-detail/components";
import { ApolloProvider } from "@apollo/react-hooks";
import ApolloClient from "apollo-boost";

export default {
  title: "Device Info view"
};

const deviceList = [
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
];

const defaults = {
  "content-type-1": "text/plain",
  "content-type-2": "text/plain",
  "content-type-3": "text/plain",
  long: 12.3544
};

const ext = {
  custom: {
    level: 0,
    serial_id: "0000",
    location: {
      long: 1.234,
      lat: 5.678
    },
    features: ["foo", "bar", "baz"]
  }
};

// const complexJson={
//     "prop_1": [
//       {
//         "prop_11": {
//           "prop_111": [
//             {
//               "prop_1111_1": "val_1111"
//             },
//             {
//               "prop_1112": "val_1112"
//             },
//             {
//               "prop_1111_2": [
//                 {
//                   "prop_11111": "val_11111"
//                 },
//                 {
//                   "prop_11112": "val_11112"
//                 }
//               ]
//             }
//           ]
//         }
//       },
//       {
//         "prop_12": "val_12"
//       }
//     ],
//     "prop_2": "val_2",
//     "prop_3": "val_3"
//   }

//   const jsonContext = {
//     k1: { a1: "v1" },
//     k2: { a1: "v1", a2: "v2" },
//     k3: [{ a1: "v1" }],
//     k4: [{ a1: "v1", a2: "v2" }],
//     k5: [{ a1: "v1" }, { b1: "w1" }],
//     k6: [
//       { a1: "v1", a2: "v2" },
//       { b1: "w1", b2: "w2" }
//     ],
//     k7: ["a", "b", "c"],
//     k8:[{a:{a1:"v1",a2:"v2"}},{b:{b1:"w1",b2:'w2'}}]
//   };

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

const dataList = [
  {
    headers: ["Message info parameter", "Type", "Value"],
    data: defaults
  },
  {
    headers: ["Basic info parameter", "Type", "Value"],
    data: ext
  }
];

const client = new ApolloClient();

export const DeviceInfoView = () => (
  <ApolloProvider client={client}>
    <Router>
      <DeviceInfo
        id={"device-info"}
        metadataList={dataList}
        deviceList={deviceList}
        credentials={credentials}
        memberOf={[]}
        viaGroups={[]}
      />
    </Router>
  </ApolloProvider>
);
