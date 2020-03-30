/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { IoTConfiguration } from "./IoTConfiguration";
import { IDropdownOption } from "components";

describe("<IoTConfiguration />", () => {
  it("shouldrenders a iot configuration component", () => {
    const onNameSpaceSelect = jest.fn(),
      handleNameChange = jest.fn(),
      handleEnabledChange = jest.fn(),
      namespaceOptions: IDropdownOption[] = [
        { key: "app1_ns", value: "app1_ns", label: "app1_ns" },
        { key: "app2_ns", value: "app2_ns", label: "app2_ns" }
      ],
      namespace = "app1_ns",
      name = "project-name",
      isNameValid = true,
      isEnabled = true;
    const { getByText } = render(
      <MemoryRouter>
        <IoTConfiguration
          onNameSpaceSelect={onNameSpaceSelect}
          handleNameChange={handleNameChange}
          handleEnabledChange={handleEnabledChange}
          namespaceOptions={namespaceOptions}
          namespace={namespace}
          name={name}
          isNameValid={isNameValid}
          isEnabled={isEnabled}
        />
      </MemoryRouter>
    );
    getByText("Project name");
    getByText("Namespace");
    getByText("Enable");
  });
});
