/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import { text, number, select } from "@storybook/addon-knobs";
import { action } from "@storybook/addon-actions";
import { AddressDetailHeader } from "../Components/AddressDetail/AddressDetailHeader";
import { AddressLinksFilter } from "src/Pages/AddressDetail/AddressLinksFilter";

export default {
  title: "Address Detail"
};

export const AddressDetailHead = () => (
  <MemoryRouter>
    <AddressDetailHeader
      storedMessages={number("Stored Messages", 1)}
      topic={null}
      type={text("Type", "Queue")}
      name={text("Name", "newqueue")}
      plan={text("Plan", "Small")}
      partitions={number("partition", 1)}
      onEdit={action("onEdit Clicked")}
      onDelete={action("onDelete Clicked")}
    />
  </MemoryRouter>
);
