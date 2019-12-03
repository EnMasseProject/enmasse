import React from "react";
import { MemoryRouter } from "react-router";
import { LinkList, ILink } from "../Components/ConnectionDetail/LinkList";

export default {
  title: "LinkList"
};

const rows: ILink[] = [
  {
    role: "sender",
    name: "foo",
    address: "queue1",
    deliveries: 123,
    rejected: 123,
    released: 123,
    modified: 123,
    presettled: 123,
    undelivered: 123,
    status: "running"
  },
  {
    role: "sender",
    name: "foo",
    address: "queue1",
    deliveries: 123,
    rejected: 123,
    released: 123,
    modified: 123,
    presettled: 123,
    undelivered: 123,
    status: "running"
  },
  {
    role: "sender",
    name: "foo",
    address: "queue1",
    deliveries: 123,
    rejected: 123,
    released: 123,
    modified: 123,
    presettled: 123,
    undelivered: 123,
    status: "running"
  }
];

export const linkList = () => (
  <MemoryRouter>
    <LinkList rows={rows} />
  </MemoryRouter>
);
