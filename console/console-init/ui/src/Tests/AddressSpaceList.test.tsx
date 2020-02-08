/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import {
  IAddressSpace,
  AddressSpaceList
} from "components/AddressSpaceList/AddressSpaceList";
import { ISortBy } from "@patternfly/react-table";

describe("Address space List", () => {
  test("it renders a list of address spaces", () => {
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
        messages: []
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
        messages: []
      }
    ];
    const handleEdit = (data: IAddressSpace) => void 0;
    const handleDelete = (data: IAddressSpace) => void 0;
    const onSort = (data: IAddressSpace) => void 0;
    const sortBy: ISortBy = { index: 0, direction: "asc" };
    const { getByText } = render(
      <MemoryRouter>
        <AddressSpaceList
          rows={addressSpaces}
          onEdit={handleEdit}
          onDelete={handleDelete}
          sortBy={sortBy}
          onSort={onSort}
          onSelectAddressSpace={() => {}}
          onSelectAllAddressSpace={() => {}}
        />
      </MemoryRouter>
    );

    //Testing elements of first row
    getByText(addressSpaces[0].name);

    //Testing elements of second row
    getByText(addressSpaces[1].name);
  });
});
