import * as React from 'react';
import { storiesOf } from "@storybook/react";
import { withKnobs, boolean, text, number, select } from "@storybook/addon-knobs";
import { MemoryRouter } from "react-router";
import { ConnectionDetailHeader } from 'src/Components/ConnectionDetail/ConnectionDetailHeader';

const stories = storiesOf("Connection Detail", module);
stories.addDecorator(withKnobs);

stories.add("Connection Header",()=>(
    <MemoryRouter>
        <ConnectionDetailHeader
            hostname={text("Container Id","1.219.2.1.33904")}
        containerId={text("hostname","myapp1")}
        protocol={text("protocol","AMQP")}
        />
    </MemoryRouter>
))