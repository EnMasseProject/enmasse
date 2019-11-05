import React from "react";
import { render, getByTitle } from "@testing-library/react";
import { Messages } from "src/Components/Messages";

describe("Messages", () => {
  test("it renders the message", () => {
    // Arrange
    const props = {
      count: 9,
      column: "MessagesIn",
      status: "running"
    };

    const { getByText } = render(<Messages {...props} />);
    const countNode = getByText(props.count.toString());

    expect(countNode).toBeDefined();
  });
});
