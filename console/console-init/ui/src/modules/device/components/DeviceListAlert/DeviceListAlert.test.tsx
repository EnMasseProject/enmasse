/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { DeviceListAlert, IDeviceListAlertProps } from "./DeviceListAlert";

describe("<DeviceListAlert />", () => {
  const props: IDeviceListAlertProps = {
    visible: true,
    variant: "info",
    title: "Run filter to view your devices",
    description: "You have a total of 36,300 devices"
  };

  it("should render an alert on top of DeviceListPage", () => {
    const { getByText } = render(<DeviceListAlert {...props} />);

    const alertTitle = getByText("Run filter to view your devices");
    const alertDescription = getByText("You have a total of 36,300 devices");

    expect(alertTitle).toBeInTheDocument();
    expect(alertDescription).toBeInTheDocument();
  });
});
