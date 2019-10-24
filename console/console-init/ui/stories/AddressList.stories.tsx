import * as React from "react";
import { MemoryRouter } from "react-router";
import { storiesOf } from "@storybook/react";
import { AddressList, IAddress } from "../src/Components/AddressSpace/AddressList";
import { action } from "@storybook/addon-actions";
import { boolean, select, withKnobs } from "@storybook/addon-knobs";
import {AddressListFilter} from "src/Components/AddressSpace/AddressListFilter";

const stories = storiesOf("Console", module);
stories.addDecorator(withKnobs);

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

stories.add("Address List Filter Component", () => {
  const [typeValue, setTypeValue] = React.useState("Queue");
  const [statusValue, setStatusValue] = React.useState("Active");
  const options = {
    Name: "Name",
    Type: "Type",
    Status: "Status"
  };

  return (
    <MemoryRouter>
      <AddressListFilter
        onSearch={action("onSearch")}
        isFilterDropdownOpen={boolean("isFilterDropdwonOpen", true)}
        onFilterSelect={action("onFilterSelect")}
        filterValue={
          select("Value Of Dropdown", options, "Name") as
            | "Name"
            | "Type"
            | "Status"
        }
        setFilterOpen={action("setFilterOpen")}
        isTypeDropdownOpen={boolean("isTypeDropdownOpen", false)}
        onTypeSelect={action("onTypeSelect")}
        typeValue={typeValue}
        setTypeOpen={action("setTypeOpen")}
        isStatusDropdownOpen={boolean("isStatusDropdownOpen", false)}
        onStatusSelect={action("onStatusSelect")}
        statusValue={statusValue}
        setStatusOpen={action("setStatusOpen")}
      />
    </MemoryRouter>
  );
});
