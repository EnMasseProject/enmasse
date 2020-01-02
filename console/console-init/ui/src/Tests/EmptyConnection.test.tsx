import * as React from "react";
import { render } from "@testing-library/react";
import { EmptyConnection } from "src/Components/AddressSpace/Connection/EmptyConnection";

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
