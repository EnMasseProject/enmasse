/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Page } from "@patternfly/react-core";
import { MemoryRouter } from "react-router";
import { ProjectCertificateList } from "modules/iot-certificates/";
import { CertificateCard } from "modules/iot-certificates";

export default {
  title: "Project Certificates"
};

const certificates = [
  {
    "subject-dn": "CN=ca,OU=Hono,O=Eclipse",
    "public-key": "PublicKey==",
    "auto-provisioning-enabled": false,
    algorithm: "RSA",
    "not-before": "2019-10-03T13:4516+02:00",
    "not-after": "2021-10-03T00:00:00Z"
  }
];

export const projectCertificate = () => (
  <CertificateCard
    setOnEditMode={() => {}}
    id="test-id"
    certificate={certificates[0]}
  />
);

export const projectCertificatePage = () => (
  <MemoryRouter>
    <Page>
      <ProjectCertificateList certificates={certificates} />
    </Page>
  </MemoryRouter>
);
