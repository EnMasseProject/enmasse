import * as React from "react";
import { storiesOf } from "@storybook/react";
import { withKnobs, boolean, text, number, select } from "@storybook/addon-knobs";
import { MemoryRouter } from "react-router";
import { AddressSpaceHeader } from "src/Components/AddressSpace/AddressSpaceHeader";
import { action } from "@storybook/addon-actions";
import { AddressSpaceNavigation } from "src/Components/AddressSpace/AddressSpaceNavigation";

const stories = storiesOf("Address Space", module);
stories.addDecorator(withKnobs);

stories.add("Address Space Header", () => (
  <MemoryRouter><br/><br/>
    <AddressSpaceHeader
      name={text("Name of Address Space", "jBoss")}
      namespace={text("Name space of Address Space", "deveops_jbosstest1")}
      type={text("type of Address Space", "Brokered")}
      createdOn={text("Number of days before it is created", "2 days ago")}
      onDownload={action("On download clicked")}
      onDelete={action("on delete clicked")}
    />
  </MemoryRouter>
));

stories.add("Address Space Header Navigation", ()=>{
    const options=["addresses","connections"];
    return(
    <MemoryRouter>
        <AddressSpaceNavigation 
        activeItem={select("Active Nav Item",options,"addresses")}
        onSelect={action("Nav Item selected")}
        />
    </MemoryRouter>
)});
