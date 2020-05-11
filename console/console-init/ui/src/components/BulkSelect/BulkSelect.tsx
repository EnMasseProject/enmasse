/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Dropdown,
  DropdownToggle,
  DropdownToggleCheckbox
} from "@patternfly/react-core";

export interface IBulkSelectProps {
  isChecked: boolean;
  isOpen: boolean;
  handleOnToggle: (val: boolean) => void;
  items: React.ReactNode[];
  selectedCount: number;
  handleOnSelect: () => void;
  handleOnChange: (val: boolean) => void;
}

export const BulkSelect: React.FunctionComponent<IBulkSelectProps> = ({
  isChecked,
  isOpen,
  items,
  selectedCount,
  handleOnChange,
  handleOnSelect,
  handleOnToggle
}) => {
  return (
    <Dropdown
      id="bulk-select-dropdown"
      onSelect={handleOnSelect}
      toggle={
        <DropdownToggle
          id="bulk-select-dropdown-toggle"
          splitButtonItems={[
            <DropdownToggleCheckbox
              id="bulk-select-checkbox"
              key="bulk-select-checkbox"
              aria-label="Select all items"
              onChange={handleOnChange}
              isChecked={isChecked}
            >
              {selectedCount} selected
            </DropdownToggleCheckbox>
          ]}
          onToggle={handleOnToggle}
        />
      }
      isOpen={isOpen}
      dropdownItems={items}
    />
  );
};
