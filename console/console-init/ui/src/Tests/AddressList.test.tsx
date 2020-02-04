/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import {
  AddressList,
  IAddress
} from "../Components/AddressSpace/Address/AddressList";
import { MemoryRouter } from "react-router";
// import { AddressListFilter } from "src/Components/AddressSpace/AddressList/AddressListFilter";

describe("Address List", () => {
  test("it renders a list of addresses", () => {
    const addresses: IAddress[] = [
      {
        name: "leo_b",
        displayName: "leo_b",
        namespace: "leo_b",
        type: "queue",
        planLabel: "small",
        planValue: "",
        messageIn: 8,
        messageOut: 9,
        storedMessages: 10,
        senders: 11,
        receivers: 12,
        partitions: 13,
        status: "running",
        isReady: true
      },
      {
        name: "newqueue",
        displayName: "newqueue",
        namespace: "newqueue",
        type: "queue",
        planLabel: "large",
        planValue: "",
        messageIn: 2,
        messageOut: 3,
        storedMessages: 4,
        senders: 5,
        receivers: 6,
        partitions: 7,
        status: "creating",
        isReady: true
      }
    ];
    const handleEdit = (data: IAddress) => void 0;
    const handleDelete = (data: IAddress) => void 0;

    const { getByText } = render(
      <MemoryRouter>
        <AddressList
          rowsData={addresses}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onSelectAddress={() => {}}
          onSelectAllAddress={() => {}}
          onPurge={() => {}}
        />
      </MemoryRouter>
    );

    //Testing elements of first row
    getByText(addresses[0].name);
    // getByText(addresses[0].type);
    getByText(addresses[0].planLabel);
    getByText(addresses[0].messageIn.toString());
    getByText(addresses[0].messageOut.toString());
    getByText(addresses[0].storedMessages.toString());
    getByText(addresses[0].senders.toString());
    getByText(addresses[0].receivers.toString());
    // getByText(addresses[0].partitions.toString());

    //Testing elements of second row
    getByText(addresses[1].name);
    // getByText(addresses[1].type;
    getByText(addresses[1].planLabel);
    getByText(addresses[1].messageIn.toString());
    getByText(addresses[1].messageOut.toString());
    getByText(addresses[1].storedMessages.toString());
    getByText(addresses[1].senders.toString());
    getByText(addresses[1].receivers.toString());
    // getByText(addresses[1].partitions.toString());
  });
});

// describe("Address List Filter", () => {
//
//   test("Test Filter options are displayed well", () => {
//     let filterValue= "Name",
//       typeValue: string | null = null,
//       filterNames: string[] = [],
//       statusValue: string | null = null;
//     const setFilterValue = (value: string | null) => {console.log(value)};
//     const setFilterNames = (value: Array<string>) => {console.log(value)};
//     const setTypeValue = (value: string | null) => {console.log(value)};
//     const setStatusValue = (value: string | null) => {console.log(value)};
//
//     const { getByText } = render(
//       <MemoryRouter>
//         <AddressListFilter
//           filterValue={filterValue}
//           setFilterValue={setFilterValue}
//           filterNames={filterNames}
//           setFilterNames={setFilterNames}
//           typeValue={typeValue}
//           setTypeValue={setTypeValue}
//           statusValue={statusValue}
//           setStatusValue={setStatusValue}
//         />
//       </MemoryRouter>
//     );
//   });
// });
