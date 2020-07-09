/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render, fireEvent } from "@testing-library/react";
import {
  IIoTCertificate,
  ICertificateFormProps,
  CertificateForm
} from "modules/iot-certificates";
import { getLabelByKey } from "utils";

describe("<CertificateForm />", () => {
  const id: string = "cf-form-test";
  const setOnEditMode = jest.fn();

  xit("should render a form to add a certificate", () => {
    const props: ICertificateFormProps = {
      setOnEditMode,
      id
    };
    const { getByText } = render(<CertificateForm {...props} />);

    getByText(getLabelByKey("subject-dn"));
    getByText(getLabelByKey("algorithm"));
    getByText(getLabelByKey("not-before"));
    getByText(getLabelByKey("not-after"));
    getByText(getLabelByKey("auto-provisioning-enabled"));
  });

  xit("should render a form to edit a certificate", () => {
    const certificate: IIoTCertificate = {
      "subject-dn": "CN=ca,OU=Hono,O=Eclipse",
      "public-key": "PublicKey==",
      "auto-provisioning-enabled": false,
      algorithm: "RSA",
      "not-before": "2019-10-03T13:4516+02:00",
      "not-after": "2021-10-03T00:00:00Z"
    };

    const props: ICertificateFormProps = {
      setOnEditMode,
      id,
      certificate
    };
    const { getByDisplayValue, getByText } = render(
      <CertificateForm {...props} />
    );

    certificate["subject-dn"] && getByDisplayValue(certificate["subject-dn"]);
    certificate["public-key"] && getByDisplayValue(certificate["public-key"]);
    typeof certificate["auto-provisioning-enabled"] === "boolean" &&
      getByText("Enabled");
    certificate["not-before"] && getByDisplayValue(certificate["not-before"]);
    certificate["not-after"] && getByDisplayValue(certificate["not-after"]);
  });

  xit("should render a form to edit a certificate with missing keys and null values", () => {
    const certificate: IIoTCertificate = {
      "subject-dn": null,
      "public-key": undefined,
      "auto-provisioning-enabled": false,
      algorithm: undefined
    };

    const props: ICertificateFormProps = {
      setOnEditMode,
      id,
      certificate
    };
    const { getByText, getByDisplayValue } = render(
      <CertificateForm {...props} />
    );

    getByText(getLabelByKey("subject-dn"));
    getByText(getLabelByKey("algorithm"));
    getByText(getLabelByKey("not-before"));
    getByText(getLabelByKey("public-key"));

    certificate["subject-dn"] && getByDisplayValue(certificate["subject-dn"]);
    certificate["public-key"] && getByDisplayValue(certificate["public-key"]);
    certificate["not-before"] && getByDisplayValue(certificate["not-before"]);
    certificate["not-after"] && getByDisplayValue(certificate["not-after"]);
  });

  xit("should reflect changes made to the text inputs", () => {
    const certificate: IIoTCertificate = {
      "subject-dn": "CN=ca,OU=Hono,O=Eclipse",
      "public-key": "PublicKey==",
      "auto-provisioning-enabled": false,
      algorithm: "RSA",
      "not-before": "2019-10-03T13:4516+02:00",
      "not-after": "2021-10-03T00:00:00Z"
    };

    const props: ICertificateFormProps = {
      setOnEditMode,
      id,
      certificate
    };

    const { getByDisplayValue } = render(<CertificateForm {...props} />);

    const subjectDnInput =
      certificate["subject-dn"] && getByDisplayValue(certificate["subject-dn"]);
    const publicKeyInput =
      certificate["public-key"] && getByDisplayValue(certificate["public-key"]);

    fireEvent.change(subjectDnInput, { target: { value: "test subject dn" } });
    getByDisplayValue("test subject dn");

    fireEvent.change(publicKeyInput, { target: { value: "test public key" } });
    getByDisplayValue("test public key");
  });
});
