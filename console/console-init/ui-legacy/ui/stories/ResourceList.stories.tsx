import * as React from "react";
import { MemoryRouter } from "react-router";
import { storiesOf } from "@storybook/react";
import { Link } from "react-router-dom";
import ResourceList, { IAddress } from "../src/Components/ResourceList";
import { action } from "@storybook/addon-actions";

const stories = storiesOf("Utils", module);

const rows: IAddress[] = [
  {
    name: "foo",
    typePlan: "small",
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
    typePlan: "small",
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
    typePlan: "small",
    messagesIn: 123,
    messagesOut: 123,
    storedMessages: 123,
    senders: 123,
    receivers: 123,
    shards: 123,
    status: "deleting"
  }
];

stories.add("Addresses ResourceList", () => (
  <MemoryRouter>
    <ResourceList
      rows={rows}
      onEdit={action("onEdit")}
      onDelete={action("onDelete")}
    />
  </MemoryRouter>
));
