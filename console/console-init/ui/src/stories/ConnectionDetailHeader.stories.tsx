/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { text, number, boolean } from "@storybook/addon-knobs";
import { MemoryRouter } from "react-router";
import { ConnectionDetailHeader } from "components/ConnectionDetail/ConnectionDetailHeader";

export default {
  title: "Connection Detail"
};

export const connectionHeader = () => (
  <MemoryRouter>
    <ConnectionDetailHeader
      hostname={text("Container Id", "1.219.2.1.33904")}
      containerId={text("hostname", "myapp1")}
      protocol={text("protocol", "AMQP")}
      product={text("product", "QpidJMS")}
      version={text("version", "0.31.0 SNAPSHOT")}
      encrypted={boolean("Encrypted", false)}
      creationTimestamp={text("creation time", "2020-01-20T11:44:28.607Z")}
      platform={text("platform", "0.8.0_152.25.125.b16, Oracle Corporation")}
      os={text("os", "Mac OS X 10.13.6,x86_64")}
      messageIn={number("messageIn", 0)}
      messageOut={number("messageOut", 0)}
    />
  </MemoryRouter>
);
