import React from "react";
import { MemoryRouter } from "react-router";
import { AddressList, IAddress } from "..//Components/AddressSpace/AddressList";
import { action } from "@storybook/addon-actions";
import { select } from "@storybook/addon-knobs";
import { AddressListFilter } from "..//Components/AddressSpace/AddressListFilter";
import { EmptyAddress } from "../Components/Common/EmptyAddress";

export default {
  title: "Address list"
};

const rows: IAddress[] = [
  {
    name: "foo",
    namespace: "foo",
    type: "Queue",
    plan: "small",
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    isReady: true,
    status: "running"
  },
  {
    name: "foo",
    namespace: "foo",
    type: "Queue",
    plan: "small",
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    isReady: true,
    status: "creating"
  },
  {
    name: "foo",
    namespace: "foo",
    type: "Queue",
    plan: "small",
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    isReady: true,
    status: "deleting"
  }
];

export const addressList = () => (
  <MemoryRouter>
    <AddressList
      rows={rows}
      onEdit={action("onEdit")}
      onDelete={action("onDelete")}
    />
  </MemoryRouter>
);

export const addressListFilterComponent = () => {
  const options = {
    Name: "Name",
    Type: "Type",
    Status: "Status"
  };

  return (
    <MemoryRouter>
      <AddressListFilter
        onSearch={action("onSearch")}
        onFilterSelect={action("onFilterSelect")}
        filterValue={
          select("Value Of Dropdown", options, "Name") as
            | "Name"
            | "Type"
            | "Status"
        }
        onTypeSelect={action("onTypeSelect")}
        typeValue={"Queue"}
        onStatusSelect={action("onStatusSelect")}
        statusValue={"Active"}
      />
    </MemoryRouter>
  );
};

export const emptyAddress = () => (
  <MemoryRouter>
    <EmptyAddress />
  </MemoryRouter>
);
