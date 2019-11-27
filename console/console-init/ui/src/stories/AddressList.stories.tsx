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
      rowsData={rows}
      onEdit={action("onEdit")}
      onDelete={action("onDelete")}
      // onCheckboxEdit={action("onCheckBoxEdit")}
      // rows={[]}
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
        setFilterValue={action("onFilterSelect")}
        filterValue={
          select("Value Of Dropdown", options, "Name") as
            | "Name"
            | "Type"
            | "Status"
        }
        setTypeValue={action("onTypeSelect")}
        typeValue={"Queue"}
        setStatusValue={action("onStatusSelect")}
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
