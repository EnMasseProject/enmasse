/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Page } from "@patternfly/react-core";
import { AddCredential } from "modules/device/components/AddCredential/AddCredential";

export default {
  title: "Add Credentials"
};

export const AddCredentials = () => {
  return (
    <Page>
      <div style={{ overflow: "auto" }}>
        <AddCredential />
      </div>
    </Page>
  );
};
