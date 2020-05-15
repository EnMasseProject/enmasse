/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { DeviceInfo } from "modules/device-detail/components";

export default {
  title: "Device Info view"
};

const deviceList = [
  { id: "device-1", name: "device-1" },
  { id: "device-2", name: "device-2" },
  { id: "device-2", name: "device-3" },
  { id: "device-1", name: "device-4" },
  { id: "device-1", name: "device-5" },
  { id: "device-2", name: "device-6" },
  { id: "device-1", name: "device-7" },
  { id: "device-2", name: "device-8" },
  { id: "device-1", name: "device-9" }
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
    type: "hashed_password",
    "auth-id": "user-1",
    enabled: false,
    secrets: [
      {
        "not-after": "2020-10-01T10:00:00Z",
        "pwd-hash": "bjb232138d",
        "pwd-function": "sha-512",
        salt: "213h"
      },
      {
        "not-before": "2020-10-01T10:00:00Z",
        "pwd-hash": "adfhk327823",
        "pwd-function": "bcrypt"
      }
    ]
  },
  {
    type: "hashed_password",
    "auth-id": "alternate-user-1",
    enabled: true,
    secrets: [
      {
        "pwd-function": "bcrypt"
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
    type: "x-509",
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

export const DeviceInfoView = () => (
  <DeviceInfo
    id={"divice-info"}
    metadataList={dataList}
    deviceList={deviceList}
    credentials={credentials}
  />
);
