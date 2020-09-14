/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Page } from "@patternfly/react-core";
import { CredentialView } from "modules/iot-device/components/CredentialView/CredentialView";
import { AddCredential } from "modules/iot-device";

export default {
  title: "Add Credentials"
};

export const CredentialsView = () => {
  return (
    <Page>
      <div style={{ overflow: "auto" }}>
        <CredentialView />
      </div>
    </Page>
  );
};
