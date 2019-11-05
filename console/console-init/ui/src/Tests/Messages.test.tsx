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
    const messageNode = getByText(props.count.toString());

    expect(messageNode.childNodes[1]).toBeDefined();
  });

  test("it renders the icon", () => {
    const props = {
      count: 9,
      column: "MessagesIn",
      status: "running"
    };

    const { getByText } = render(<Messages {...props} />);
    const messageNode = getByText(props.count.toString());

    expect(messageNode.childNodes[0]).toBeDefined();
  });
});
