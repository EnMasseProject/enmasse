/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import ReactDom from "react-dom";
import { MemoryRouter } from "react-router";
import { render, cleanup } from "@testing-library/react";
import {
  AddressSpaceList,
  IAddressSpace,
  getTableColumns,
  getTableCells
} from "modules/messaging-project";

afterEach(cleanup);

describe("<AddressSpaceList/>", () => {
  const addressSpaces: IAddressSpace[] = [
    {
      name: "sample_1",
      nameSpace: "app_ns_1",
      creationTimestamp: "2009-06-15T13:45:30",
      type: "Brokered",
      displayName: "sample_namespace_1",
      planValue: "brokered-medium",
      isReady: true,
      status: "creating",
      phase: "Active",
      messages: [],
      authenticationService: "authservice"
    },
    {
      name: "sample_2",
      nameSpace: "app_ns_2",
      creationTimestamp: "2009-06-15T13:45:30",
      type: "Standard",
      displayName: "sample_namespace_2",
      planValue: "standard-medium",
      isReady: true,
      status: "running",
      phase: "Active",
      messages: [],
      authenticationService: "authservice"
    }
  ];

  const tableRows = addressSpaces.map(row => getTableCells(row));
  const props = {
    rows: tableRows,
    cells: getTableColumns,
    onSort: jest.fn(),
    onSelect: jest.fn()
  };

  it("should render without crashing", () => {
    const div = document.createElement("div");
    ReactDom.render(
      <MemoryRouter>
        <AddressSpaceList {...props} />
      </MemoryRouter>,
      div
    );
  });

  test("should render a list of address spaces", () => {
    const { getByText } = render(
      <MemoryRouter>
        <AddressSpaceList {...props} />
      </MemoryRouter>
    );

    //first row's data
    expect(getByText(addressSpaces[0].type)).toBeInTheDocument();
    expect(getByText(addressSpaces[0].name)).toBeInTheDocument();
    //second row's data
    expect(getByText(addressSpaces[1].type)).toBeInTheDocument();
    expect(getByText(addressSpaces[1].name)).toBeInTheDocument();
  });
});
