/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { Dropdown, DropdownToggle, DropdownItem } from "@patternfly/react-core";
import React, { useState } from "react";
import { CaretDownIcon } from "@patternfly/react-icons";

export const ProjectToolbar: React.FunctionComponent<{}> = () => {
  const [isOpen, setIsOpen] = useState(false);
  const onSelect = () => {
    setIsOpen(!isOpen);
  };
  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };
  const dropdownItems = [
    <DropdownItem key="link">Input Device info</DropdownItem>,
    <DropdownItem key="action" component="button">
      Upload a JSON file
    </DropdownItem>
  ];
  return (
    <Dropdown
      onSelect={onSelect}
      toggle={
        <DropdownToggle
          id="toggle-id"
          onToggle={onToggle}
          iconComponent={CaretDownIcon}
        >
          Add Device
        </DropdownToggle>
      }
      isOpen={isOpen}
      dropdownItems={dropdownItems}
    />
  );
};
