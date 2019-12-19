import React from "react";
import { MemoryRouter } from "react-router";
import {
  ConnectionList,
  IConnection
} from "../Components/AddressSpace/Connection/ConnectionList";
import { EmptyConnection } from "../Components/Common/EmptyConnection";

export default {
  title: "Connection"
};

const rows: IConnection[] = [
  {
    hostname: "foo",
    containerId: "123",
    protocol: "AMQP",
    encrypted: true,
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
    encrypted: true,
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
    encrypted: true,
    messagesIn: 123,
    messagesOut: 123,
    senders: 123,
    receivers: 123,
    status: "running"
  }
];

export const connectionList = () => (
  <MemoryRouter>
    <ConnectionList rows={rows} />
  </MemoryRouter>
);

export const emptyConnectionList = () => (
  <MemoryRouter>
    <EmptyConnection />
  </MemoryRouter>
);
