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
import { ToolbarContent, Toolbar, ToolbarItem } from "@patternfly/react-core";
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
}

export const DeviceListToolbar: React.FunctionComponent<IDeviceListToolbarProps &
  ICreateDeviceButtonProps> = ({
  handleInputDeviceInfo,
  handleJSONUpload,
  onSelectAllDevices,
  isChecked,
  kebabItems
}) => {
  return (
    <Toolbar id="device-data-toolbar" data-codemods="true">
      <ToolbarContent id="device-data-toolbar-content">
        <ToolbarItem
          variant="bulk-select"
          id="device-list-toolbar-item-1"
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
          id="device-list-toolbar-item-2"
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
          id="device-list-toolbar-item-3"
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
      </ToolbarContent>
    </Toolbar>
  );
};
