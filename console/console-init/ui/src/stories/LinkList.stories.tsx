import React from "react";
import { MemoryRouter } from "react-router";
import { LinkList, ILink } from "../Components/ConnectionDetail/LinkList";
import { text, number, select } from "@storybook/addon-knobs";
import { action } from "@storybook/addon-actions";
import { ConnectionLinksFilter } from "src/Pages/ConnectionDetail/ConnectionLinksFilter";

export default {
  title: "Connection Detail"
};

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

export const linkList = () => (
  <MemoryRouter>
    <LinkList rows={rows} />
  </MemoryRouter>
);

// export const ConnectionLinksFilterStory = () => {
//   const options=["","Sender","Receiver"];
//   return(
//   <MemoryRouter>
//     <ConnectionLinksFilter
//       filterValue={text("FilterValue", "Name")}
//       setFilterValue={action("set filter value")}
//       filterNames={
//         text("filter name", "") != "" ? [text("filter name", "")] : []
//       }
//       setFilterNames={action("setFilterNames")}
//       filterAddresses={
//         text("filter address", "") != "" ? [text("filter address", "")] : []
//       }
//       setFilterAddresses={action("setFilterAddress")}
//       filterRole={select("Role ", options, "")}
//       setFilterRole={action("setFilterRole")}
//       totalLinks={number("total links", 1)}
//       setSortValue={()=>{}}
//     />
//   </MemoryRouter>
//   );
// };
