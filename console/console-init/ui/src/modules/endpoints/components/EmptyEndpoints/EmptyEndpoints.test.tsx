/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { EmptyEndpoints } from "./EmptyEndpoints";

describe("Empty State for Connection List", () => {
  test("it renders empty address state instead of list of addresses", () => {
    const { getByText } = render(<EmptyEndpoints />);

    getByText("No endpoints");
    getByText("You currently don't have any endpoints");
  });
});
