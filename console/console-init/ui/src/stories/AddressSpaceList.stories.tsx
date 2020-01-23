/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { storiesOf } from "@storybook/react";
import { withKnobs } from "@storybook/addon-knobs";
import {
  IAddressSpace,
  AddressSpaceList
} from "src/Components/AddressSpaceList/AddressSpaceList";
import { MemoryRouter } from "react-router";
import { action } from "@storybook/addon-actions";

const stories = storiesOf("Address Space List", module);
stories.addDecorator(withKnobs);

const rows: IAddressSpace[] = [
  {
    name: "jupiter_as1",
    nameSpace: "app1_ns",
    creationTimestamp: "2019-11-10T05:08:31.489Z",
    type: "standard",
    displayName: "Small",
    isReady: false
  },
  {
    name: "saturn-as2",
    nameSpace: "app1_ns",
    creationTimestamp: "2019-11-10T05:10:41.297Z",
    type: "brokered",
    displayName: "Small",
    isReady: true
  },
  {
    name: "mars_as2",
    nameSpace: "app2_ns",
    creationTimestamp: "2019-11-10T18:37:56.975Z",
    type: "standard",
    displayName: "Large",
    isReady: true
  },
  {
    name: "earth_as3",
    nameSpace: "myapp_ns",
    creationTimestamp: "2019-11-10T15:08:32.489Z",
    type: "brokered",
    displayName: "Medium",
    isReady: false
  }
];

stories.add("Address Space List", () => (
  <MemoryRouter>
    <AddressSpaceList
      rows={rows}
      onEdit={action("onEdit")}
      onDelete={action("onDelete")}
      setSelectedAddressSpaces={action("OnSelect")}
    />
  </MemoryRouter>
));
