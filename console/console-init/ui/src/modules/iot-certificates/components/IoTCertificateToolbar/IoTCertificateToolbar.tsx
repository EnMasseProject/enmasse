/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  DataToolbar,
  DataToolbarContent,
  DataToolbarItem,
  Button
} from "@patternfly/react-core";
import { SwitchWithToggle } from "components";

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

  return (
    <DataToolbar id="pct-data-toolbar">
      <DataToolbarContent>
        <DataToolbarItem>
          <Button
            id="pct-add-certificate-button"
            onClick={handleAddCertificateClick}
          >
            Add certificate
          </Button>
        </DataToolbarItem>
        <DataToolbarItem>
          <Button
            id="pct-upload-certificate-button"
            variant="link"
            onClick={handleUploadCertificateClick}
          >
            Upload certificate
          </Button>
        </DataToolbarItem>
        <DataToolbarItem
          breakpointMods={[{ modifier: "align-right", breakpoint: "md" }]}
        >
          <SwitchWithToggle
            id="pct-edit-json-switch"
            label="Edit in Json"
            isChecked={isJsonView}
            onChange={handleJsonViewChange}
          />
        </DataToolbarItem>
      </DataToolbarContent>
    </DataToolbar>
  );
};
