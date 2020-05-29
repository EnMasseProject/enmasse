/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  IProjectCertificate,
  CertificateForm,
  CertificateCard
} from "modules/iot-certificates";

export interface IProjectCertificateProps {
  certificate: IProjectCertificate;
  id: string;
}

export const ProjectCertificate: React.FunctionComponent<IProjectCertificateProps> = ({
  certificate,
  id
}) => {
  const [onEditMode, setOnEditMode] = useState<boolean>(false);

  return (
    <>
      {onEditMode ? (
        <CertificateForm
          id={id}
          setOnEditMode={setOnEditMode}
          certificate={certificate}
        />
      ) : (
        <CertificateCard
          id={id}
          setOnEditMode={setOnEditMode}
          certificate={certificate}
        />
      )}
    </>
  );
};
