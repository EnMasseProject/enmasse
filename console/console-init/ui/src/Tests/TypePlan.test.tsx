import React from "react";
import { render, getByTitle } from "@testing-library/react";
import { TypePlan } from "../Components/TypePlan";

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
