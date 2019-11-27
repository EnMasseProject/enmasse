import React from "react";
import { render } from "@testing-library/react";
import { AddressList, IAddress } from "../Components/AddressSpace/AddressList";
import { MemoryRouter } from "react-router";

describe("Address List", () => {
  test("it renders a list of addresses", () => {
    const addresses: IAddress[] = [
      {
        name: "leo_b",
        namespace: "leo_b",
        type: "Queue",
        plan: "small",
        messagesIn: 8,
        messagesOut: 9,
        storedMessages: 10,
        senders: 11,
        receivers: 12,
        shards: 13,
        status: "running",
        isReady: true
      },
      {
        name: "newqueue",
        namespace: "newqueue",
        type: "Random",
        plan: "large",
        messagesIn: 2,
        messagesOut: 3,
        storedMessages: 4,
        senders: 5,
        receivers: 6,
        shards: 7,
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
        />
      </MemoryRouter>
    );

    //Testing elements of first row
    getByText(addresses[0].name);
    // getByText(addresses[0].type;
    getByText(addresses[0].plan);
    getByText(addresses[0].messagesIn.toString());
    getByText(addresses[0].messagesOut.toString());
    getByText(addresses[0].storedMessages.toString());
    getByText(addresses[0].senders.toString());
    getByText(addresses[0].receivers.toString());
    getByText(addresses[0].shards.toString());

    //Testing elements of second row
    getByText(addresses[1].name);
    // getByText(addresses[1].type;
    getByText(addresses[1].plan);
    getByText(addresses[1].messagesIn.toString());
    getByText(addresses[1].messagesOut.toString());
    getByText(addresses[1].storedMessages.toString());
    getByText(addresses[1].senders.toString());
    getByText(addresses[1].receivers.toString());
    getByText(addresses[1].shards.toString());
  });
});
