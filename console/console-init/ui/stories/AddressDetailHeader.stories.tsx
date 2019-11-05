import * as React from "react";
import { storiesOf } from "@storybook/react";
import { withKnobs, text, select, number, color } from "@storybook/addon-knobs";
import { MemoryRouter } from "react-router";
import { action } from "@storybook/addon-actions";
import { AddressDetailHeader } from "src/Components/AddressDetail/AddressDetailHeader";

const stories = storiesOf("Address Detail", module);
stories.addDecorator(withKnobs);

stories.add("Address Detail Header",()=>(
    <MemoryRouter>
        <AddressDetailHeader 
        type={text("Type","Queue")}
        name={text("Name","newqueue")}
        plan={text("Plan","Small")}
        shards={number("shard",1)}
        onEdit={action("onEdit Clicked")}
        onDelete={action("onDelete Clicked")}
        />
    </MemoryRouter>
))
