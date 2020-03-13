/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import { AddressList, IAddress } from "modules/address/components/AddressList";
import { action } from "@storybook/addon-actions";
// import { AddressListFilter } from "..//Components/AddressSpace/AddressListFilter";
import { EmptyAddress } from "modules/address/components/EmptyAddress";

export default {
  title: "Address list"
};

const rows: IAddress[] = [
  {
    name: "foo.juu",
    displayName: "juu",
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
    status: "running",
    creationTimestamp: "2020-02-28T14:32:39.985Z"
  },
  {
    name: "foo.hui",
    displayName: "hui",
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
    status: "creating",
    creationTimestamp: "2020-02-26T14:32:39.985Z"
  },
  {
    name: "foo.ganymede",
    displayName: "ganymede",
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
    status: "deleting",
    creationTimestamp: "2020-02-27T14:32:39.985Z"
  }
];

export const addressList = () => (
  <MemoryRouter>
    <AddressList
      rowsData={rows}
      onEdit={action("onEdit")}
      onDelete={action("onDelete")}
      onPurge={action("onPurge")}
      onSelectAddress={action("select address")}
      onSelectAllAddress={action("SelectAllAddress")}
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
