/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import { action } from "@storybook/addon-actions";
import { AddressListFilter } from "modules/address/containers/AddressListFilter";
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
