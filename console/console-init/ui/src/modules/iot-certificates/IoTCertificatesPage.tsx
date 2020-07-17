/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import { useDocumentTitle, useA11yRouteChange } from "use-patternfly";
import { IIoTCertificate, IoTCertificateList } from "./components";

const defaultCertificates = [
  {
    "subject-dn": "CN=ca,OU=Hono,O=Eclipse",
    "public-key": "PublicKey==",
    "auto-provisioning-enabled": true,
    algorithm: "RSA",
    "not-before": "2019-10-03T13:4516+02:00",
    "not-after": "2021-10-03T00:00:00Z"
  },
  {
    "subject-dn": "CN=ca,OU=Hono,O=Eclipse",
    "not-after": "2021-10-03T00:00:00Z"
  },
  {
    "not-after": "2021-10-03T00:00:00Z",
    "auto-provisioning-enabled": undefined,
    "subject-dn": null,
    "not-before": null,
    algorithm: null,
    "public-key": null
  }
];
export default function IoTCertificates() {
  console.log(":ASFSAFA");
  useDocumentTitle("IoT Certificates");
  useA11yRouteChange();
  //   const { name, namespace } = useParams();
  const [certificates, setCertificates] = useState<IIoTCertificate[]>(
    // TODO: remove this default Certificates once query get fininished
    defaultCertificates
  );

  useEffect(() => {
    setCertificates(certificates);
  }, [certificates]);

  //TODO: Add query to fetch certificates
  const onSave = (certificate: IIoTCertificate) => {};

  const onCreate = (certificate: IIoTCertificate) => {
    //Query to edit certificate
  };

  const onDelete = (certificate: IIoTCertificate) => {
    // Query to delete certificate
  };

  const onChangeStatus = (certificate: IIoTCertificate, isEnabled: boolean) => {
    // Query for enable certificate
  };

  return (
    <IoTCertificateList
      certificates={certificates}
      onSave={onSave}
      onCreate={onCreate}
      onDelete={onDelete}
      onChangeStatus={onChangeStatus}
    />
  );
}
