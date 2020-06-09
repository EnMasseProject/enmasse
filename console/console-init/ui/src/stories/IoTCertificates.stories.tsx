/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Page } from "@patternfly/react-core";
import { MemoryRouter } from "react-router";
import { IoTCertificateList } from "modules/iot-certificates/";
import { CertificateCard } from "modules/iot-certificates";
import { action } from "@storybook/addon-actions";

export default {
  title: "IoT Certificates"
};

const certificates = [
  {
    "subject-dn": "CN=ca,OU=Hono,O=Eclipse",
    "public-key": "PublicKey==",
    "auto-provisioning-enabled": true,
    algorithm: "RSA",
    "not-before": "2019-10-03T13:4516+02:00",
    "not-after": "2021-10-03T00:00:00Z"
  },
  {
    "subject-dn": "CN=ca,OU=Hono,O=Eclipse",
    "not-after": "2021-10-03T00:00:00Z"
  },
  {
    "not-after": "2021-10-03T00:00:00Z",
    "auto-provisioning-enabled": undefined,
    "subject-dn": null,
    "not-before": null,
    algorithm: null,
    "public-key": null
  }
];

export const iotCertificate = () => (
  <>
    <CertificateCard
      setOnEditMode={action("Edit mode for card clicked")}
      id="test-id"
      certificate={certificates[0]}
      onDelete={action("onDelete click")}
      onEnableOrDisable={action("onEnableOrDisable click")}
    />
  </>
);

export const iotCertificatePage = () => (
  <MemoryRouter>
    <Page>
      <IoTCertificateList
        certificates={certificates}
        onSave={action("onSave click")}
        onCreate={action("onCreate click")}
        onDelete={action("onDelete click")}
        onEnableOrDisable={action("onEnableOrDisable click")}
      />
    </Page>
  </MemoryRouter>
);
