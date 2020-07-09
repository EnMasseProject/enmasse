/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render, fireEvent } from "@testing-library/react";
import {
  IoTCertificate,
  IIoTCertificateProps,
  IIoTCertificate
} from "modules/iot-certificates";
import { getLabelByKey } from "utils";

describe("<IoTCertificate />", () => {
  const id: string = "ic-test";

  const certificate: IIoTCertificate = {
    "subject-dn": "CN=ca,OU=Hono,O=Eclipse",
    "public-key": "PublicKey==",
    "auto-provisioning-enabled": false,
    algorithm: "RSA",
    "not-before": "2019-10-03T13:4516+02:00",
    "not-after": "2021-10-03T00:00:00Z"
  };

  const props: IIoTCertificateProps = {
    certificate,
    id
  };

  xit("should render an IoT certificate, initially a card", () => {
    const { getByText } = render(<IoTCertificate {...props} />);

    getByText(getLabelByKey("subject-dn"));
    certificate["subject-dn"] && getByText(certificate["subject-dn"]);

    getByText(getLabelByKey("not-before"));
    certificate["not-before"] && getByText(certificate["not-before"]);

    getByText(getLabelByKey("auto-provisioning-enabled"));
    getByText("Disabled");
  });

  xit("should render an editable IoT certificate", () => {
    const { getByLabelText, getByText, getByDisplayValue } = render(
      <IoTCertificate {...props} />
    );

    // Click the actions kebab
    const actionBtnNode = getByLabelText("Actions");
    fireEvent.click(actionBtnNode);

    // Click the edit certificate option from dropdown
    const editBtnNode = getByLabelText("Edit certificate");
    fireEvent.click(editBtnNode);

    // Test the buttons, labels and input values of form
    getByText("Save");
    getByText("Cancel");

    getByText(getLabelByKey("subject-dn"));
    getByText(getLabelByKey("algorithm"));
    getByText(getLabelByKey("not-before"));

    certificate["subject-dn"] && getByDisplayValue(certificate["subject-dn"]);
    certificate["algorithm"] && getByText(certificate["algorithm"]);
    certificate["not-before"] && getByDisplayValue(certificate["not-before"]);
  });
});
