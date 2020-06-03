/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import {
  IIoTCertificateToolbarProps,
  IoTCertificateToolbar
} from "modules/iot-certificates";

describe("<IoTCertificateToolbar />", () => {
  const props: IIoTCertificateToolbarProps = {
    setShowCertificateForm: jest.fn(),
    isJsonView: false,
    handleJsonViewChange: jest.fn()
  };

  const { getByText, getAllByText } = render(
    <IoTCertificateToolbar {...props} />
  );

  it("should render the toolbar for IoT certificates", () => {
    getByText("Add certificate");
    getByText("Upload certificate");
    getAllByText("Edit in Json");
  });
});
