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
        product={text("product","QpidJMS")}
        version={text("version","0.31.0 SNAPSHOT")}
        platform={text("platform","0.8.0_152.25.125.b16, Oracle Corporation")}
        os={text("os","Mac OS X 10.13.6,x86_64")}
        messagesIn={number("messagesIn",0)}
        messagesOut={number("messagesOut",0)}
        />
    </MemoryRouter>
))