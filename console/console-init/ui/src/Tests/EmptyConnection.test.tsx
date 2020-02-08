/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { render } from "@testing-library/react";
import { EmptyConnection } from "components/AddressSpace/Connection/EmptyConnection";

describe("Empty State for Connection List", () => {
  test("it renders empty address state instead of list of addresses", () => {
    const { getByText } = render(<EmptyConnection />);

    const headerNode = getByText("No connections");
    const descriptionNode = getByText(
      "You currently don't have any connections"
    );

    expect(headerNode).toBeDefined();
    expect(descriptionNode).toBeDefined();
  });
});
