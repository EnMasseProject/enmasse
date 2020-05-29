/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  ProjectCertificateToolbar,
  CertificateForm,
  ProjectCertificate
} from "modules/iot-certificates";
import { GridItem, Grid } from "@patternfly/react-core";

export interface IProjectCertificate {
  "subject-dn": string;
  "public-key": string;
  "auto-provisioning-enabled": boolean;
  algorithm: string;
  "not-before": string;
  "not-after": string;
}

export interface IProjectCertificateListProps {
  certificates: IProjectCertificate[];
}

export const ProjectCertificateList: React.FunctionComponent<IProjectCertificateListProps> = ({
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
        <ProjectCertificateToolbar
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
        {certificates.map((certificate: IProjectCertificate, index: number) => (
          <ProjectCertificate
            key={`certificate-${index}`}
            id={`certificate-${index}`}
            certificate={certificate}
          />
        ))}
      </GridItem>
    </Grid>
  );
};
