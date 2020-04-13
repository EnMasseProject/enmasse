/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import { render } from "@testing-library/react";
import { EmptyAddress } from "./EmptyAddress";

describe("Empty Page with Empty state for Addresses", () => {
  test("it renders empty address state instead of list of addresses", () => {
    const { getByText } = render(
      <MemoryRouter>
        <EmptyAddress />
      </MemoryRouter>
    );

    const descriptionNode = getByText(
      "There are currently no addresses available. Please click on the button below to create one.Learn more about this in the"
    );
    const buttonNode = getByText("Create Address");
    const documentLinkNode = getByText("documentation");
    const headerNode = getByText("Create an address");

    expect(descriptionNode).toBeDefined();
    expect(buttonNode).toBeDefined();
    expect(documentLinkNode).toBeDefined();
    expect(headerNode).toBeDefined();
  });
});
