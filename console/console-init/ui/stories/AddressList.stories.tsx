import * as React from "react";
import { MemoryRouter } from "react-router";
import { storiesOf } from "@storybook/react";
import { AddressList, IAddress } from "../src/Components/AddressList";
import { action } from "@storybook/addon-actions";

const stories = storiesOf("Console", module);

const rows: IAddress[] = [
  {
    name: "foo",
    type: "Queue",
    plan: "small",
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    status: "running"
  },
  {
    name: "foo",
    type: "Queue",
    plan: "small",
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    status: "creating"
  },
  {
    name: "foo",
    type: "Queue",
    plan: "small",
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    status: "deleting"
  }
];

stories.add("Address List", () => (
  <MemoryRouter>
    <AddressList
      rows={rows}
      onEdit={action("onEdit")}
      onDelete={action("onDelete")}
    />
  </MemoryRouter>
));
