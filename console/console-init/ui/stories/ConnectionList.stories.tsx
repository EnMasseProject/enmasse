import * as React from "react";
import { MemoryRouter } from "react-router";
import { storiesOf } from "@storybook/react";
import { ConnectionList, IConnection } from "../src/Components/AddressSpace/ConnectionList";
import {  EmptyConnection } from "src/Components/Common/EmptyConnection";

const stories = storiesOf("Connection", module);

const rows: IConnection[] = [
  {
    hostname: "foo",
    containerId: "123",
    protocol: "AMQP",
    messagesIn: 123,
    messagesOut: 123,
    senders: 123,
    receivers: 123,
    status: "running"
  },
  {
    hostname: "foo",
    containerId: "123",
    protocol: "AMQP",
    messagesIn: 123,
    messagesOut: 123,
    senders: 123,
    receivers: 123,
    status: "running"
  },
  {
    hostname: "foo",
    containerId: "123",
    protocol: "AMQP",
    messagesIn: 123,
    messagesOut: 123,
    senders: 123,
    receivers: 123,
    status: "running"
  }
];

stories.add("Connection List", () => (
  <MemoryRouter>
    <ConnectionList rows={rows} />
  </MemoryRouter>
));

stories.add("Empty Connection List",()=>(
  <MemoryRouter>
    <EmptyConnection />
  </MemoryRouter>
))
