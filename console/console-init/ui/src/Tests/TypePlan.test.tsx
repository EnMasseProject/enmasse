/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render, getByTitle } from "@testing-library/react";
import { TypePlan } from "modules/address/components/TypePlan";

describe("TypePlan", () => {
  test("it renders the plan", () => {
    // Arrange
    const props = {
      plan: "Small",
      type: "Queue"
    };

    const { getByText } = render(<TypePlan {...props} />);
    const planNode = getByText(props.plan);

    expect(planNode).toBeDefined();
  });

  test("it renders the type", () => {
    const props = {
      plan: "Random",
      type: "T"
    };

    const { getByText } = render(<TypePlan {...props} />);
    const typeNode = getByText(props.type);

    expect(typeNode).toBeDefined();
  });
});
