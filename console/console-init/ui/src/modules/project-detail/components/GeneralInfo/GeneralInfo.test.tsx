/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { GeneralInfo, IInfoTypePlan } from "./GeneralInfo";
import { kFormatter } from "utils";

describe("<GeneralInfo />", () => {
  it("should renders a general info", () => {
    const eventAddressName: IInfoTypePlan = {
      type: "qpid-jms:sender"
    };
    const telemetryAddressName: IInfoTypePlan = {
      type: "qpid-jms:sender"
    };
    const commandAddressName: IInfoTypePlan = {
      type: "qpid-jms:sender",
      plan: "Reciever-156458"
    };
    const props = {
      addressSpace: "devops_iot",
      eventAddressName: eventAddressName,
      telemetryAddressName: telemetryAddressName,
      commandAddressName: commandAddressName,
      maxConnection: 50000,
      dataVolume: 50000,
      startDate: "",
      endDate: ""
    };
    const { getByText } = render(
      <MemoryRouter>
        <GeneralInfo {...props} />
      </MemoryRouter>
    );
    getByText("General Info");
    getByText("Address space");
    getByText("Event address name");
    getByText("Telemetry address name");
    getByText("Command address name");
    getByText("Max Connection");
    getByText("Data Volume");
    getByText("Start Date");
    getByText("End Date");
  });
});
