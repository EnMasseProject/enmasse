/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import {
  IAddressSpace,
  AddressSpaceList,
  IObjectMeta_v1_Input,
  getActionResolver,
  getTableColumns,
  getTableCells
} from "modules/address-space";
import { ISortBy, IRowData } from "@patternfly/react-table";

describe("AddressSpaceList", () => {
  test("should render a list of address spaces", () => {
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
    const totalItemsCount = 2;
    const tableRows = addressSpaces.map(row => getTableCells(row));
    const onChangeEdit = (data: IAddressSpace) => void 0;
    const onChangeDelete = (data: IAddressSpace) => void 0;
    const onDownloadCertificate = (data: IObjectMeta_v1_Input) => void 0;
    const onSort = (data: IAddressSpace) => void 0;
    const onSelect = () => void 0;
    const sortBy: ISortBy = { index: 0, direction: "asc" };
    const actionResolver = (rowData: IRowData) => {
      return getActionResolver(
        rowData,
        onChangeEdit,
        onChangeDelete,
        onDownloadCertificate
      );
    };

    const { getByText } = render(
      <MemoryRouter>
        <AddressSpaceList
          rows={tableRows}
          cells={getTableColumns}
          actionResolver={actionResolver}
          sortBy={sortBy}
          onSort={onSort}
          onSelect={onSelect}
          totalItemsCount={totalItemsCount}
        />
      </MemoryRouter>
    );

    //Testing elements of first row
    const type = getByText(addressSpaces[0].type);
    expect(type).toBeDefined();
  });
});
