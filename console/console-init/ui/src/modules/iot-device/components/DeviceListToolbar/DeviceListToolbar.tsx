/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  DropdownWithBulkSelect,
  DropdownWithKebabToggle,
  IDropdownWithBulkSelectProps
} from "components";
import {
  ToolbarContent,
  Toolbar,
  ToolbarItem,
  Button
} from "@patternfly/react-core";
import {
  CreateDeviceButton,
  ICreateDeviceButtonProps
} from "modules/iot-device/components";

export interface IDeviceListToolbarProps
  extends Omit<
    IDropdownWithBulkSelectProps,
    "dropdownId" | "dropdownToggleId" | "checkBoxId" | "ariaLabel"
  > {
  kebabItems: React.ReactNode[];
  onSelectAllDevices: (val: boolean) => void;
  onChange: (val: boolean) => void;
  handleToggleModal: () => void;
}

export const DeviceListToolbar: React.FunctionComponent<IDeviceListToolbarProps &
  ICreateDeviceButtonProps> = ({
  handleInputDeviceInfo,
  handleJSONUpload,
  onSelectAllDevices,
  isChecked,
  kebabItems,
  handleToggleModal
}) => {
  return (
    <>
      <Toolbar id="device-data-toolbar" data-codemods="true">
        <ToolbarContent id="device-data-toolbar-content">
          <ToolbarItem
            variant="bulk-select"
            id="device-list-toolbar-bulk-select"
            key="bulk-select"
            aria-label="Select multiple devices"
            data-codemods="true"
          >
            <DropdownWithBulkSelect
              dropdownId="device-bulk-select"
              dropdownToggleId="device-bulk-select-toggle"
              checkBoxId="device-bulk-select-checkbox"
              ariaLabel="Bulk select dropdown for device list"
              isChecked={isChecked}
              onChange={onSelectAllDevices}
            />
          </ToolbarItem>
          <ToolbarItem
            id="device-list-toolbar-create-device-button"
            key="create-device"
            aria-label="Create device button"
            data-codemods="true"
          >
            <CreateDeviceButton
              handleInputDeviceInfo={handleInputDeviceInfo}
              handleJSONUpload={handleJSONUpload}
            />
          </ToolbarItem>
          <ToolbarItem
            id="device-list-toolbar-kebab-dropdown"
            key="kebab-dropdown"
            aria-label="Device list kebab dropdown"
            data-codemods="true"
          >
            <DropdownWithKebabToggle
              isPlain={true}
              dropdownItems={kebabItems}
              id="device-list-toolbar-kebab"
            />
          </ToolbarItem>
          <ToolbarItem>
            <Button variant="link" onClick={handleToggleModal}>
              Manage columns
            </Button>
          </ToolbarItem>
        </ToolbarContent>
      </Toolbar>
    </>
  );
};
