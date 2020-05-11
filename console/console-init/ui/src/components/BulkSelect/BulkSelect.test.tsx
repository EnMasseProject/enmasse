/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { BulkSelect, IBulkSelectProps } from "./BulkSelect";
import { DropdownItem } from "@patternfly/react-core";

describe("<BulkDelete />", () => {
  const items: React.ReactNode[] = [
    <DropdownItem key="item-1" onClick={jest.fn()}>
      Select none (0 items)
    </DropdownItem>,
    <DropdownItem key="item-2" onClick={jest.fn()}>
      Select page items
    </DropdownItem>,
    <DropdownItem key="item-3" onClick={jest.fn()}>
      Select all items
    </DropdownItem>
  ];

  const props: IBulkSelectProps = {
    isChecked: false,
    isOpen: false,
    handleOnToggle: jest.fn(),
    selectedCount: 20,
    items,
    handleOnChange: jest.fn(),
    handleOnSelect: jest.fn()
  };

  it("should render a checkbox dropdown for multiple selection", () => {
    const { getByText } = render(<BulkSelect {...props} />);

    const checkBoxDisplay = getByText("20 selected");

    expect(checkBoxDisplay).toBeInTheDocument();
  });

  it("should render the dropdown items when opened", () => {
    props["isOpen"] = true;

    const { getByText } = render(<BulkSelect {...props} />);

    const dropDownItem0 = getByText("Select none (0 items)");
    const dropDownItem1 = getByText("Select page items");
    const dropDownItem2 = getByText("Select all items");

    expect(dropDownItem0).toBeInTheDocument();
    expect(dropDownItem1).toBeInTheDocument();
    expect(dropDownItem2).toBeInTheDocument();
  });
});
