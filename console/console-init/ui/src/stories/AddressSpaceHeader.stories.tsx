import React from "react";
import { text, select } from "@storybook/addon-knobs";
import { MemoryRouter } from "react-router";
import { AddressSpaceHeader } from "../Components/AddressSpace/AddressSpaceHeader";
import { action } from "@storybook/addon-actions";
import { AddressSpaceNavigation } from "../Components/AddressSpace/AddressSpaceNavigation";

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
