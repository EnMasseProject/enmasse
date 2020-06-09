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
import {
  GridItem,
  Grid,
  PageSection,
  PageSectionVariants
} from "@patternfly/react-core";

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
  onSave: (certificate: IIoTCertificate) => void;
  onCreate: (certificate: IIoTCertificate) => void;
  onDelete: (certificate: IIoTCertificate) => void;
  onEnableOrDisable: (certificate: IIoTCertificate, isEnabled: boolean) => void;
}

export const IoTCertificateList: React.FunctionComponent<IIoTCertificateListProps> = ({
  certificates,
  onSave,
  onCreate,
  onDelete,
  onEnableOrDisable
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
    <PageSection variant={PageSectionVariants.default}>
      <Grid key={"iiid"}>
        <GridItem span={8}>
          <PageSection style={{ paddingBottom: 0, paddingTop: 0 }}>
            <IoTCertificateToolbar
              handleJsonViewChange={handleJsonViewChange}
              isJsonView={isJsonView}
              setShowCertificateForm={setShowCertificateForm}
            />
          </PageSection>
          {showCertificateForm && (
            <PageSection style={{ paddingBottom: 0 }}>
              <CertificateForm
                id="pcl-add-certificate-form"
                setOnEditMode={setShowCertificateForm}
                onSave={onCreate}
              />
              <br />
            </PageSection>
          )}
          {certificates.map((certificate: IIoTCertificate, index: number) => (
            <IoTCertificate
              key={`certificate-${index}`}
              id={`certificate-${index}`}
              certificate={certificate}
              onEdit={onSave}
              onEnableOrDisable={onEnableOrDisable}
              onDelete={onDelete}
            />
          ))}
        </GridItem>
        <GridItem span={4}> </GridItem>
      </Grid>
    </PageSection>
  );
};
