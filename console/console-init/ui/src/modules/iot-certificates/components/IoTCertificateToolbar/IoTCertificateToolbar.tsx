/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Button
} from "@patternfly/react-core";
import { SwitchWithToggle } from "components";
import { StyleSheet, css } from "aphrodite";

export interface IIoTCertificateToolbarProps {
  setShowCertificateForm: React.Dispatch<React.SetStateAction<boolean>>;
  isJsonView: boolean;
  handleJsonViewChange: (val: boolean) => void;
}

export const IoTCertificateToolbar: React.FunctionComponent<IIoTCertificateToolbarProps> = ({
  setShowCertificateForm,
  isJsonView,
  handleJsonViewChange
}) => {
  const handleAddCertificateClick = () => {
    setShowCertificateForm(true);
  };

  const handleUploadCertificateClick = () => {
    // TODO: Mechanism to upload a certificate
  };

  const styles = StyleSheet.create({
    toolbar_body: {
      display: "none"
    }
  });

  return (
    <Toolbar
      className={isJsonView ? css(styles.toolbar_body) : "none"}
      id="iot-cert-toolbar"
      data-codemods="true"
    >
      <ToolbarContent>
        <ToolbarItem data-codemods="true">
          <Button
            id="iot-cert-add-button"
            aria-label="Add Certificate"
            onClick={handleAddCertificateClick}
          >
            Add certificate
          </Button>
        </ToolbarItem>
        <ToolbarItem data-codemods="true">
          <Button
            id="iot-cert-upload-button"
            aria-label="Upload Certificate"
            variant="link"
            onClick={handleUploadCertificateClick}
          >
            Upload certificate
          </Button>
        </ToolbarItem>
        <ToolbarItem alignment={{ md: "alignRight" }} data-codemods="true">
          <SwitchWithToggle
            id="iot-cert-edit-json-switch"
            aria-label="Switch for edit in Json"
            label="Edit in Json"
            isChecked={isJsonView}
            onChange={handleJsonViewChange}
          />
        </ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  );
};
