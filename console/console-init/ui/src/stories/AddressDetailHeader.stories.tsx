import React from "react";
import { MemoryRouter } from "react-router";
import { text, number } from "@storybook/addon-knobs";
import { action } from "@storybook/addon-actions";
import { AddressDetailHeader } from "../Components/AddressDetail/AddressDetailHeader";

export default {
    title: 'Address Detail'
};

export const sample = () => (
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
);
