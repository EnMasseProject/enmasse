/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  DropdownWithBulkSelect,
  TablePagination,
  DropdownWithKebabToggle,
  IDropdownWithBulkSelectProps
} from "components";
import {
  DataToolbarContent,
  DataToolbar,
  DataToolbarItem,
  PaginationProps
} from "@patternfly/react-core";
import {
  CreateDeviceButton,
  ICreateDeviceButtonProps
} from "modules/device/components";

export interface IDeviceListToolbarProps
  extends ICreateDeviceButtonProps,
    PaginationProps,
    Omit<
      IDropdownWithBulkSelectProps,
      "dropdownId" | "dropdownToggleId" | "checkBoxId" | "ariaLabel"
    > {
  kebabItems: React.ReactNode[];
}

export const DeviceListToolbar: React.FunctionComponent<IDeviceListToolbarProps> = ({
  itemCount,
  perPage,
  page,
  onSetPage,
  onPerPageSelect,
  handleInputDeviceInfo,
  handleJSONUpload,
  isOpen,
  handleOnSelect,
  handleOnToggle,
  isChecked,
  items,
  handleOnChange,
  kebabItems
}) => {
  return (
    <DataToolbar id="device-data-toolbar">
      <DataToolbarContent id="device-data-toolbar-content">
        <DataToolbarItem
          variant="bulk-select"
          id="device-list-toolbar-item-1"
          key="bulk-select"
          aria-label="Select multiple devices"
        >
          <DropdownWithBulkSelect
            dropdownId="device-bulk-select"
            dropdownToggleId="device-bulk-select-toggle"
            checkBoxId="device-bulk-select-checkbox"
            ariaLabel="Bulk select dropdown for device list"
            isOpen={isOpen}
            handleOnSelect={handleOnSelect}
            handleOnToggle={handleOnToggle}
            isChecked={isChecked}
            items={items}
            handleOnChange={handleOnChange}
          />
        </DataToolbarItem>
        <DataToolbarItem
          id="device-list-toolbar-item-2"
          key="create-device"
          aria-label="Create device button"
        >
          <CreateDeviceButton
            handleInputDeviceInfo={handleInputDeviceInfo}
            handleJSONUpload={handleJSONUpload}
          />
        </DataToolbarItem>
        <DataToolbarItem
          id="device-list-toolbar-item-3"
          key="kebab-dropdown"
          aria-label="Device list kebab dropdown"
        >
          <DropdownWithKebabToggle
            isPlain={true}
            dropdownItems={kebabItems}
            id="device-list-toolbar-kebab"
          />
        </DataToolbarItem>
        <DataToolbarItem
          id="device-list-toolbar-item-4"
          variant="pagination"
          key="pagination"
          breakpointMods={[{ modifier: "align-right", breakpoint: "md" }]}
          aria-label="Device List pagination"
        >
          <TablePagination
            id="device-list-pagination"
            itemCount={itemCount}
            perPage={perPage}
            page={page}
            onSetPage={onSetPage}
            onPerPageSelect={onPerPageSelect}
          />
        </DataToolbarItem>
      </DataToolbarContent>
    </DataToolbar>
  );
};
