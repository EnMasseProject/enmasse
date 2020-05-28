/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { IoTReview } from "./IoTReview";

describe("IoT Review", () => {
  test("it renders a iot review", () => {
    const props = {
      name: "iot-ptoject",
      namespace: "iot-namespace",
      isEnabled: true
    };
    const { getByText } = render(
      <MemoryRouter>
        <IoTReview {...props} />
      </MemoryRouter>
    );
    getByText("Project name");
    getByText("Namespace");
    getByText("Enabled");
    getByText(props.name);
    getByText(props.namespace);
    getByText(props.isEnabled.toString());
  });
});
