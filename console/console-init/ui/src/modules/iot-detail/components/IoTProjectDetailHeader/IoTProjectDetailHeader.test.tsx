/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { IoTProjectDetailHeader } from "./IoTProjectDetailHeader";

describe("IoT Project header", () => {
  test("it renders a iot project header", () => {
    const props = {
      projectName: "iot-project-name",
      type: "iot-project-type",
      status: "Active",
      isEnabled: true,
      changeEnable: () => {},
      onEdit: () => {},
      onDelete: () => {}
    };
    const { getByText } = render(
      <MemoryRouter>
        <IoTProjectDetailHeader {...props} />
      </MemoryRouter>
    );
    getByText("Type :");
    getByText("Status :");
    getByText(props.projectName);
    getByText(props.type);
    getByText(props.status);
  });
});
