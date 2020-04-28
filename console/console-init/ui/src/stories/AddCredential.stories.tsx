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
