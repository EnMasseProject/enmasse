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

export interface IDropdownWithBulkSelectProps {
  dropdownId: string;
  dropdownToggleId: string;
  checkBoxId: string;
  ariaLabel: string;
  isChecked: boolean;
  isOpen: boolean;
  handleOnToggle: (val: boolean) => void;
  items: React.ReactNode[];
  handleOnSelect: () => void;
  handleOnChange: (val: boolean) => void;
}

export const DropdownWithBulkSelect: React.FunctionComponent<IDropdownWithBulkSelectProps> = ({
  dropdownId,
  ariaLabel,
  dropdownToggleId,
  checkBoxId,
  isChecked,
  isOpen,
  items,
  handleOnChange,
  handleOnSelect,
  handleOnToggle
}) => {
  return (
    <Dropdown
      id={dropdownId}
      onSelect={handleOnSelect}
      toggle={
        <DropdownToggle
          id={dropdownToggleId}
          splitButtonItems={[
            <DropdownToggleCheckbox
              id={checkBoxId}
              key="bulk-select-checkbox"
              aria-label={ariaLabel}
              onChange={handleOnChange}
              isChecked={isChecked}
            />
          ]}
          onToggle={handleOnToggle}
        />
      }
      isOpen={isOpen}
      dropdownItems={items}
    />
  );
};
