/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import {
  ConnectionList,
  IConnection
} from "../Components/AddressSpace/Connection/ConnectionList";
import { EmptyConnection } from "../Components/AddressSpace/Connection/EmptyConnection";

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
