/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  IIoTCertificate,
  CertificateForm,
  CertificateCard
} from "modules/iot-certificates";
import { PageSection } from "@patternfly/react-core";

export interface IIoTCertificateProps {
  certificate: IIoTCertificate;
  onEdit: (certificate: IIoTCertificate) => void;
  onDelete: (certificate: IIoTCertificate) => void;
  onEnableOrDisable: (certificate: IIoTCertificate, isEnabled: boolean) => void;
  id: string;
}

export const IoTCertificate: React.FunctionComponent<IIoTCertificateProps> = ({
  certificate,
  onEdit,
  onDelete,
  onEnableOrDisable,
  id
}) => {
  const [onEditMode, setOnEditMode] = useState<boolean>(false);

  return (
    <PageSection style={{ paddingBottom: 0 }}>
      {onEditMode ? (
        <CertificateForm
          id={id}
          setOnEditMode={setOnEditMode}
          certificate={certificate}
          onSave={onEdit}
        />
      ) : (
        <CertificateCard
          id={id}
          setOnEditMode={setOnEditMode}
          certificate={certificate}
          onDelete={onDelete}
          onEnableOrDisable={onEnableOrDisable}
        />
      )}
    </PageSection>
  );
};
