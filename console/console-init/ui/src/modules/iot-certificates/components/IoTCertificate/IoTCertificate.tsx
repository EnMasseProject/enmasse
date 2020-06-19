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
import { ICertificateCardProps } from "../CertificateCard";
import { StyleSheet, css } from "aphrodite";

export interface IIoTCertificateProps
  extends Pick<
    ICertificateCardProps,
    "certificate" | "onDelete" | "onChangeStatus" | "id"
  > {
  onEdit: (certificate: IIoTCertificate) => void;
}

const style = StyleSheet.create({
  no_bottom_padding: {
    paddingBottom: 0
  }
});

export const IoTCertificate: React.FunctionComponent<IIoTCertificateProps> = ({
  certificate,
  onEdit,
  onDelete,
  onChangeStatus,
  id
}) => {
  const [onEditMode, setOnEditMode] = useState<boolean>(false);

  return (
    <PageSection className={css(style.no_bottom_padding)}>
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
          onChangeStatus={onChangeStatus}
        />
      )}
    </PageSection>
  );
};
