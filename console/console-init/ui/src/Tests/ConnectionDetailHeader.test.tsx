/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  IConnectionHeaderDetailProps,
  ConnectionDetailHeader
} from "components/ConnectionDetail/ConnectionDetailHeader";
import { render, fireEvent } from "@testing-library/react";

describe("Connection Detail Header with all connection details", () => {
  test("it renders ConnectionDetailHeader component with all props details at top of the page", () => {
    const props: IConnectionHeaderDetailProps = {
      hostname: "myapp1",
      creationTimestamp: "2020-01-20T11:44:28.607Z",
      containerId: "1.219.2.1.33904",
      protocol: "AMQP",
      product: "QpidJMS",
      version: "0.31.0",
      encrypted: true,
      platform: "0.8.0_152.25.125.b16, Oracle Corporation",
      os: "Mac OS X 10.13.6,x86_64",
      messageIn: 2,
      messageOut: 1
    };

    const { getByText } = render(
      <ConnectionDetailHeader {...props} addressSpaceType={"standard"} />
    );
    getByText(props.hostname);
    getByText(props.containerId);
    getByText(props.protocol);
    const seeMoreNode = getByText("See more details");
    fireEvent.click(seeMoreNode);

    getByText("Hide details");
    getByText(props.product + "");
    getByText(props.version + "");
    getByText(props.platform + "");
    getByText(props.os + "");
    getByText(props.messageIn + " Message in/sec");
    getByText(props.messageOut + " Message out/sec");
  });
});
