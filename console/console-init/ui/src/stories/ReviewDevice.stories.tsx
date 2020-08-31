/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Page } from "@patternfly/react-core";
import { ReviewDevice, IDeviceProp } from "modules/iot-device";
import { text } from "@storybook/addon-knobs";

export default {
  title: "Review Device"
};

export const ReviewDevicePage = () => {
  const initialDevice: IDeviceProp = {
    deviceInformation: {
      deviceId: "DEVICE ID",
      status: false,
      metadata: []
    },
    connectionType: "N/A",
    gateways: {
      gateways: ["Device-1"],
      gatewayGroups: []
    },
    credentials: [
      {
        "auth-id": "authId password 1",
        secrets: [
          {
            "pwd-hash": "pwd-test",
            comment: "was just for testing",
            "not-after": "2020-10-01T10:00:00Z",
            "not-before": "2020-10-01T10:00:00Z"
          }
        ],
        enabled: true,
        type: "password",
        ext: { "para-1": "value-1", "para-2": "value2", "para-3": "value-3" }
      },
      {
        "auth-id": "authId psk 2",
        secrets: [
          {
            "pwd-hash": "pwd-test",
            comment: "was just for testing",
            "not-after": "2020-10-01T10:00:00Z",
            "not-before": "2020-10-01T10:00:00Z"
          }
        ],
        enabled: false,
        type: "psk",
        ext: { "para-6": "value-6", "para-7": "value7", "para-8": "value-8" }
      }
    ]
  };
  return (
    <Page>
      <ReviewDevice
        title={text(
          "Title",
          "Verify that the following information is correct before done"
        )}
        device={initialDevice}
      />
    </Page>
  );
};
