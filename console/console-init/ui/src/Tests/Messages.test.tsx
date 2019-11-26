import React from "react";
import { render, getByTitle } from "@testing-library/react";
import { Messages } from "../Components/Common/Messages";

describe("Messages", () => {
  test("it renders the message", () => {
    // Arrange
    const props = {
      count: 9,
      column: "MessagesIn",
      isReady: true
    };

    const { getByText } = render(<Messages {...props} />);
    const messageNode = getByText(props.count.toString());

    expect(messageNode).toBeDefined();
  });

  test("it renders the icon", () => {
    const props = {
      count: 9,
      column: "MessagesIn",
      isReady: true
    };

    const { getByText } = render(<Messages {...props} />);
    const messageNode = getByText(props.count.toString());

    expect(messageNode).toBeDefined();
  });
});
