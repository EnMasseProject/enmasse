import * as React from "react";
import { MemoryRouter } from "react-router";
import { storiesOf } from "@storybook/react";
import { LinkList, ILink } from "../src/Components/LinkList";

const stories = storiesOf("Console", module);

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

stories.add("Link List", () => (
  <MemoryRouter>
    <LinkList rows={rows} />
  </MemoryRouter>
));
