/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { text, select } from "@storybook/addon-knobs";
import { MemoryRouter } from "react-router";
import { action } from "@storybook/addon-actions";
import {
  MessagingProjectHeader,
  MessagingProjectNavigation
} from "modules/messaging-project/components";

export default {
  title: "Messaging Project"
};

export const addressSpaceHeader = () => (
  <MemoryRouter>
    <MessagingProjectHeader
      name={text("Name of Messaging Project", "jBoss")}
      namespace={text("Name space of Messaging Project", "deveops_jbosstest1")}
      type={text("type of Messaging Project", "Brokered")}
      createdOn={text("Time of creation", "2020-04-20T15:08:32.489Z")}
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
      <MessagingProjectNavigation
        activeItem={select("Active Nav Item", options, "addresses")}
      />
    </MemoryRouter>
  );
};
