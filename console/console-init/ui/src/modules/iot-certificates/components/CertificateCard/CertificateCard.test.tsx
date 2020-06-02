/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import {
  IIoTCertificate,
  ICertificateCardProps,
  CertificateCard
} from "modules/iot-certificates";
import { getLabelByKey } from "utils";

describe("<CertificateCard />", () => {
  const id = "cc-test";

  const setOnEditMode = jest.fn();

  it("should render a card for certificate", () => {
    const certificate: IIoTCertificate = {
      "subject-dn": "CN=ca,OU=Hono,O=Eclipse",
      "public-key": "PublicKey==",
      "auto-provisioning-enabled": false,
      algorithm: "RSA",
      "not-before": "2019-10-03T13:4516+02:00",
      "not-after": "2021-10-03T00:00:00Z"
    };

    const props: ICertificateCardProps = {
      certificate,
      id,
      setOnEditMode
    };

    const { getByText } = render(<CertificateCard {...props} />);

    getByText(getLabelByKey("subject-dn"));
    certificate["subject-dn"] && getByText(certificate["subject-dn"]);

    getByText(getLabelByKey("not-before"));
    certificate["not-before"] && getByText(certificate["not-before"]);

    getByText(getLabelByKey("auto-provisioning-enabled"));
    getByText("Disabled");

    getByText("algorithm");
    getByText("RSA");
  });

  it("should render a card for certificate with missing keys", () => {
    const certificate = {
      algorithm: "RSA",
      "not-before": "2019-10-03T13:4516+02:00",
      "not-after": "2021-10-03T00:00:00Z"
    };

    const props: ICertificateCardProps = {
      certificate,
      id,
      setOnEditMode
    };

    const { getByText } = render(<CertificateCard {...props} />);

    getByText(getLabelByKey("auto-provisioning-enabled"));
    getByText("Enabled");
  });

  it("should render a card for certificate with null values", () => {
    const certificate = {
      algorithm: "RSA",
      "not-before": "2019-10-03T13:4516+02:00",
      "not-after": "2021-10-03T00:00:00Z"
    };

    const props: ICertificateCardProps = {
      certificate,
      id,
      setOnEditMode
    };

    const { getByText } = render(<CertificateCard {...props} />);

    getByText(getLabelByKey("auto-provisioning-enabled"));
    getByText("Enabled");
  });
});
