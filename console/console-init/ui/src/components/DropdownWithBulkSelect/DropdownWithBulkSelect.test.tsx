/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import {
  DropdownWithBulkSelect,
  IDropdownWithBulkSelectProps
} from "./DropdownWithBulkSelect";
import { DropdownItem } from "@patternfly/react-core";

describe("<DropdownWithBulkSelect />", () => {
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

  const props: IDropdownWithBulkSelectProps = {
    dropdownId: "bulk-select-dropdown",
    dropdownToggleId: "bulk-select-dropdown",
    checkBoxId: "bulk-select-checkbox",
    ariaLabel: "Select multiple items",
    isChecked: false,
    isOpen: false,
    handleOnToggle: jest.fn(),
    items,
    handleOnChange: jest.fn(),
    handleOnSelect: jest.fn()
  };

  it("should render the dropdown items when opened", () => {
    props["isOpen"] = true;

    const { getByText } = render(<DropdownWithBulkSelect {...props} />);

    const dropDownItem0 = getByText("Select none (0 items)");
    const dropDownItem1 = getByText("Select page items");
    const dropDownItem2 = getByText("Select all items");

    expect(dropDownItem0).toBeInTheDocument();
    expect(dropDownItem1).toBeInTheDocument();
    expect(dropDownItem2).toBeInTheDocument();
  });
});
