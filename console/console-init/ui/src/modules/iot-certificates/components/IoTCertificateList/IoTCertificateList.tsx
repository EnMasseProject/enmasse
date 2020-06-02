/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  IoTCertificateToolbar,
  CertificateForm,
  IoTCertificate
} from "modules/iot-certificates";
import { GridItem, Grid } from "@patternfly/react-core";

export interface IIoTCertificate {
  "subject-dn"?: string | null;
  "public-key"?: string | null;
  "auto-provisioning-enabled"?: boolean | null;
  algorithm?: string | null;
  "not-before"?: string | null;
  "not-after"?: string | null;
}

export interface IIoTCertificateListProps {
  certificates: IIoTCertificate[];
}

export const IoTCertificateList: React.FunctionComponent<IIoTCertificateListProps> = ({
  certificates
}) => {
  const [showCertificateForm, setShowCertificateForm] = useState<boolean>(
    false
  );
  const [isJsonView, setIsJsonView] = useState<boolean>(false);

  const handleJsonViewChange = (value: boolean) => {
    setIsJsonView(value);

    // TODO: Show data in JSON Editor
  };

  return (
    <Grid key={"iiid"}>
      <GridItem span={6}>
        <IoTCertificateToolbar
          handleJsonViewChange={handleJsonViewChange}
          isJsonView={isJsonView}
          setShowCertificateForm={setShowCertificateForm}
        />
        <br />
        {showCertificateForm && (
          <>
            <CertificateForm
              id="pcl-add-certificate-form"
              setOnEditMode={setShowCertificateForm}
            />
            <br />
          </>
        )}
        {certificates.map((certificate: IIoTCertificate, index: number) => (
          <IoTCertificate
            key={`certificate-${index}`}
            id={`certificate-${index}`}
            certificate={certificate}
          />
        ))}
      </GridItem>
    </Grid>
  );
};
