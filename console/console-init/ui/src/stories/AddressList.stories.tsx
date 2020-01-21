/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import {
  AddressList,
  IAddress
} from "../Components/AddressSpace/Address/AddressList";
import { action } from "@storybook/addon-actions";
import { select } from "@storybook/addon-knobs";
// import { AddressListFilter } from "..//Components/AddressSpace/AddressListFilter";
import { EmptyAddress } from "../Components/AddressSpace/Address/EmptyAddress";

export default {
  title: "Address list"
};

const rows: IAddress[] = [
  {
    name: "foo",
    namespace: "foo",
    type: "Queue",
    planLabel: "small",
    planValue: "standard-small-queue",
    messageIn: 123,
    messageOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    partitions: 123,
    isReady: true,
    status: "running"
  },
  {
    name: "foo",
    namespace: "foo",
    type: "Queue",
    planLabel: "small",
    planValue: "standard-small-queue",
    messageIn: 123,
    messageOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    partitions: 123,
    isReady: true,
    status: "creating"
  },
  {
    name: "foo",
    namespace: "foo",
    type: "Queue",
    planLabel: "small",
    planValue: "standard-small-queue",
    messageIn: 123,
    messageOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    partitions: 123,
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

export const emptyAddress = () => {
  const [isOpen, setIsOpen] = React.useState<boolean>(false);
  return (
    <MemoryRouter>
      <EmptyAddress isWizardOpen={isOpen} setIsWizardOpen={setIsOpen} />
    </MemoryRouter>
  );
};
