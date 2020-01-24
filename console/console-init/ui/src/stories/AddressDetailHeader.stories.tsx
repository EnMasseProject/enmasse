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
      type={text("Type", "Queue")}
      name={text("Name", "newqueue")}
      plan={text("Plan", "Small")}
      partitions={number("partition", 1)}
      onEdit={action("onEdit Clicked")}
      onDelete={action("onDelete Clicked")}
    />
  </MemoryRouter>
);

// export const AddressLinksFilterStory = () => {
//   const options=["","Sender","Receiver"];
//   return(
//   <MemoryRouter>
//     <AddressLinksFilter
//       filterValue={text("FilterValue", "Name")}
//       setFilterValue={action("set filter value")}
//       filterNames={
//         text("filter name", "") != "" ? [text("filter name", "")] : []
//       }
//       setFilterNames={action("setFilterNames")}
//       filterContainers={
//         text("filter address", "") != "" ? [text("filter address", "")] : []
//       }
//       setFilterContainers={action("setFilterAddress")}
//       filterRole={select("Role ", options, "")}
//       setFilterRole={action("setFilterRole")}
//       totalLinks={number("total links", 1)}
//       setSortValue={()=>{}}
//     />
//   </MemoryRouter>
//   );
// };
