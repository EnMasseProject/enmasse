/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { text, select } from "@storybook/addon-knobs";
import { MemoryRouter } from "react-router";
import { AddressSpaceHeader } from "components/AddressSpace/AddressSpaceHeader";
import { action } from "@storybook/addon-actions";
import { AddressSpaceNavigation } from "components/AddressSpace/AddressSpaceNavigation";

export default {
  title: "Address Space"
};

export const addressSpaceHeader = () => (
  <MemoryRouter>
    <AddressSpaceHeader
      name={text("Name of Address Space", "jBoss")}
      namespace={text("Name space of Address Space", "deveops_jbosstest1")}
      type={text("type of Address Space", "Brokered")}
      createdOn={text("Number of days before it is created", "2 days ago")}
      onDownload={action("On download clicked")}
      onDelete={action("on delete clicked")}
      onEdit={action("on Edit clicked")}
    />
  </MemoryRouter>
);

export const addressSpaceHeaderNavigation = () => {
  const options = ["addresses", "connections"];
  return (
    <MemoryRouter>
      <AddressSpaceNavigation
        activeItem={select("Active Nav Item", options, "addresses")}
      />
    </MemoryRouter>
  );
};
