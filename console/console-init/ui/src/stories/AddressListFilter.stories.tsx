import React from "react";
import { MemoryRouter } from "react-router";
import { action } from "@storybook/addon-actions";
import { AddressListFilter } from "src/Components/AddressSpace/Address/AddressListFilter";
import { text, number } from "@storybook/addon-knobs";

export default {
    title: "Address List Filter"
  };
  
export const addressListFilter = () => (
  <MemoryRouter>
    <AddressListFilter
      filterValue={text("FilterValue", "Name")}
      setFilterValue={action("set filter value")}
      filterNames={[text("Filter Names", "")]}
      setFilterNames={action("set Filter names")}
      typeValue={text("typeValue", "")}
      setTypeValue={action("setTypeValue")}
      statusValue={text("statusValue", "")}
      setStatusValue={action("setStatusValue")}
      totalAddresses={number("totalAddresses", 1)}
    />
  </MemoryRouter>
);
