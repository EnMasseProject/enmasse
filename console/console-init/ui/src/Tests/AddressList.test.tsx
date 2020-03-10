/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { AddressList, IAddress } from "modules/address/components/AddressList";
import { MemoryRouter } from "react-router";

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
        isReady: true,
        creationTimestamp: "2020-02-28T14:32:39.985Z"
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
        isReady: true,
        creationTimestamp: "2020-02-28T14:32:39.985Z"
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
