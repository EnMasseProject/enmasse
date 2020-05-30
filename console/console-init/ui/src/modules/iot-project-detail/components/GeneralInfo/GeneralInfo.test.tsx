/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { GeneralInfo } from "./GeneralInfo";

describe("<GeneralInfo />", () => {
  it("should renders a general info", () => {
    const eventAddressName: Array<string> = ["qpid-jms:sender"];
    const telemetryAddressName: Array<string> = ["qpid-jms:sender"];
    const commandAddressName: Array<string> = ["qpid-jms:sender"];
    const props = {
      addressSpace: "devops_iot",
      eventAddresses: eventAddressName,
      telemetryAddresses: telemetryAddressName,
      commandAddresses: commandAddressName,
      maxConnection: 50000,
      dataVolume: 50000,
      startDate: "",
      endDate: "",
      namespace: "namespace"
    };
    const { getByText } = render(
      <MemoryRouter>
        <GeneralInfo {...props} />
      </MemoryRouter>
    );
    getByText("General Info");
    getByText("Address space");
    getByText("Events address name");
    getByText("Telemetry address name");
    getByText("Command address name");
    getByText("Max Connection");
    getByText("Data Volume");
    getByText("Start Date");
    getByText("End Date");
  });
});
