/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Dropdown, DropdownToggle, DropdownItem } from "@patternfly/react-core";
import { CaretDownIcon } from "@patternfly/react-icons";

export interface ICreateDeviceButtonProps {
  handleInputDeviceInfo: () => void;
  handleJSONUpload: () => void;
}

export const CreateDeviceButton: React.FunctionComponent<ICreateDeviceButtonProps> = ({
  handleInputDeviceInfo,
  handleJSONUpload
}) => {
  const [isOpen, setIsOpen] = useState<boolean>(false);

  const dropdownItems = [
    <DropdownItem
      onClick={handleInputDeviceInfo}
      id="cd-dropdown-item-1"
      key="device-info"
      component="button"
    >
      Input device info
    </DropdownItem>,
    <DropdownItem
      onClick={handleJSONUpload}
      id="cd-dropdown-item-2"
      key="json-file"
      component="button"
    >
      Upload a JSON file
    </DropdownItem>
  ];

  return (
    <Dropdown
      id="create-device-dropdown"
      onSelect={() => setIsOpen(!isOpen)}
      toggle={
        <DropdownToggle
          onToggle={() => setIsOpen(!isOpen)}
          iconComponent={CaretDownIcon}
          isPrimary
          id="create-device-toggle"
        >
          Add device
        </DropdownToggle>
      }
      isOpen={isOpen}
      dropdownItems={dropdownItems}
    />
  );
};
