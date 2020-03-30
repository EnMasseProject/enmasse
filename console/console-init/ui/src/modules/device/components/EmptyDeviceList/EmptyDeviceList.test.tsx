/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render, fireEvent } from "@testing-library/react";
import { EmptyDeviceList, IEmptyDeviceListProps } from "./EmptyDeviceList";

describe("<EmptyDeviceList />", () => {
  const props: IEmptyDeviceListProps = {
    handleInputDeviceInfo: jest.fn(),
    handleJSONUpload: jest.fn()
  };

  it("should render an empty address state instead of list of addresses", () => {
    const { getByText } = render(<EmptyDeviceList {...props} />);

    const headerNode = getByText("No Devices");
    const bodyNode = getByText("don't have any devices here", { exact: false });
    const createDeviceBtn = getByText("Add device");

    expect(headerNode).toBeInTheDocument();
    expect(bodyNode).toBeInTheDocument();
    expect(createDeviceBtn).toBeInTheDocument();

    fireEvent.click(createDeviceBtn);
  });
});
