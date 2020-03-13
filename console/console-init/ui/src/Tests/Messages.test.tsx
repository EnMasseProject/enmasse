/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render, getByTitle } from "@testing-library/react";
import { Messages } from "modules/address/components/Messages";

describe("Messages", () => {
  test("it renders the message", () => {
    // Arrange
    const props = {
      count: 9,
      column: "MessageIn",
      isReady: true
    };

    const { getByText } = render(<Messages {...props} />);
    const messageNode = getByText(props.count.toString());

    expect(messageNode).toBeDefined();
  });

  test("it renders the icon", () => {
    const props = {
      count: 9,
      column: "MessageIn",
      isReady: true
    };

    const { getByText } = render(<Messages {...props} />);
    const messageNode = getByText(props.count.toString());

    expect(messageNode).toBeDefined();
  });
});
