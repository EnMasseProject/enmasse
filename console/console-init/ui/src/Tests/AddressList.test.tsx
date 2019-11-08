import React from "react";
import { render } from "@testing-library/react";
import { AddressList, IAddress } from "../Components/AddressSpace/AddressList";
import { MemoryRouter } from "react-router";

describe("Address List", () => {
  test("it renders a list of addresses", () => {
    const addresses: IAddress[] = [
      {
        name: "leo_b",
        type: "Queue",
        plan: "small",
        messagesIn: 8,
        messagesOut: 9,
        storedMessages: 10,
        senders: 11,
        receivers: 12,
        shards: 13,
        status: "running"
      },
      {
        name: "newqueue",
        type: "Random",
        plan: "large",
        messagesIn: 2,
        messagesOut: 3,
        storedMessages: 4,
        senders: 5,
        receivers: 6,
        shards: 7,
        status: "creating"
      }
    ];
    const handleEdit = (data: IAddress) => void 0;
    const handleDelete = (data: IAddress) => void 0;

    const { getByText } = render(
      <MemoryRouter>
        <AddressList
          rows={addresses}
          onEdit={handleEdit}
          onDelete={handleDelete}
        />
      </MemoryRouter>
    );

    //Testing elements of first row
    const nameNodeOne = getByText(addresses[0].name);
    const typeNodeOne = getByText(addresses[0].type);
    const planNodeOne = getByText(addresses[0].plan);
    const messagesInNodeOne = getByText(addresses[0].messagesIn.toString());
    const messagesOutNodeOne = getByText(addresses[0].messagesOut.toString());
    const storedMessagesNodeOne = getByText(
      addresses[0].storedMessages.toString()
    );
    const sendersNodeOne = getByText(addresses[0].senders.toString());
    const receiversNodeOne = getByText(addresses[0].receivers.toString());
    const shardsNodeOne = getByText(addresses[0].shards.toString());

    expect(nameNodeOne).toBeDefined();
    expect(typeNodeOne).toBeDefined();
    expect(planNodeOne).toBeDefined();
    expect(messagesInNodeOne).toBeDefined();
    expect(messagesOutNodeOne).toBeDefined();
    expect(storedMessagesNodeOne).toBeDefined();
    expect(sendersNodeOne).toBeDefined();
    expect(receiversNodeOne).toBeDefined();
    expect(shardsNodeOne).toBeDefined();

    //Testing elements of second row
    const nameNodeTwo = getByText(addresses[1].name);
    const typeNodeTwo = getByText(addresses[1].type);
    const planNodeTwo = getByText(addresses[1].plan);
    const messagesInNodeTwo = getByText(addresses[1].messagesIn.toString());
    const messagesOutNodeTwo = getByText(addresses[1].messagesOut.toString());
    const storedMessagesNodeTwo = getByText(
      addresses[1].storedMessages.toString()
    );
    const sendersNodeTwo = getByText(addresses[1].senders.toString());
    const receiversNodeTwo = getByText(addresses[1].receivers.toString());
    const shardsNodeTwo = getByText(addresses[1].shards.toString());

    expect(nameNodeTwo).toBeDefined();
    expect(typeNodeTwo).toBeDefined();
    expect(planNodeTwo).toBeDefined();
    expect(messagesInNodeTwo).toBeDefined();
    expect(messagesOutNodeTwo).toBeDefined();
    expect(storedMessagesNodeTwo).toBeDefined();
    expect(sendersNodeTwo).toBeDefined();
    expect(receiversNodeTwo).toBeDefined();
    expect(shardsNodeTwo).toBeDefined();
  });
});
