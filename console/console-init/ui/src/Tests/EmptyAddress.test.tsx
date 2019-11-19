import * as React from "react";
import { render } from "@testing-library/react";
import { EmptyAddress } from "src/Components/Common/EmptyAddress";

describe("Empty Page with Empty state for Addresses", () => {
  test("it renders empty address state instead of list of addresses", () => {
    const { getByText } = render(<EmptyAddress />);

    const descriptionNode = getByText(
      "There are currently no addresses available. Please click on the button below to create one.Learn more about this on the"
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
