/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render, fireEvent } from "@testing-library/react";
import {
  CreateDeviceButton,
  ICreateDeviceButtonProps
} from "modules/device/components";

describe("<CreateDeviceButton />", () => {
  const props: ICreateDeviceButtonProps = {
    handleInputDeviceInfo: jest.fn(),
    handleJSONUpload: jest.fn()
  };

  it("should render a dropdown with options to create a device", () => {
    const { getByText } = render(<CreateDeviceButton {...props} />);

    const createBtn = getByText("Add device");

    expect(createBtn).toBeInTheDocument();

    fireEvent.click(createBtn);

    const inputDeviceBtn = getByText("Input device info");
    const inputJSONBtn = getByText("Upload a JSON file");

    expect(inputDeviceBtn).toBeInTheDocument();
    expect(inputJSONBtn).toBeInTheDocument();
  });
});
