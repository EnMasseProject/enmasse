/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { ToolbarItem, Toolbar, ToolbarContent } from "@patternfly/react-core";
import { ISortBy } from "@patternfly/react-table";
import { SortForMobileView, useWindowDimensions } from "components";
import {
  AddressLinksToggleGroup,
  IAddressLinksToggleGroupProps
} from "modules/address-detail/components";

export interface IAddressLinksToolbarProps
  extends IAddressLinksToggleGroupProps {
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onClearAllFilters: () => void;
}
const AddressLinksToolbar: React.FunctionComponent<IAddressLinksToolbarProps &
  DataToolbarContentProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  containerSelected,
  containerInput,
  containerOptions,
  roleSelected,
  selectedNames,
  selectedContainers,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onContainerSelect,
  onContainerClear,
  onRoleSelect,
  onSearch,
  onDelete,
  sortValue,
  setSortValue,
  onClearAllFilters,
  onChangeNameInput,
  onChangeContainerInput,
  setNameInput,
  setContainerInput
}) => {
  const { width } = useWindowDimensions();
  const sortMenuItems = [
    { key: "name", value: "Name", index: 2 },
    { key: "deliveryRate", value: "DeliveryRate", index: 3 },
    { key: "backlog", value: "Backlog", index: 4 }
  ];
  const toolbarItems = (
    <>
      <AddressLinksToggleGroup
        totalRecords={totalRecords}
        filterSelected={filterSelected}
        nameSelected={nameSelected}
        nameInput={nameInput}
        containerSelected={containerSelected}
        containerInput={containerInput}
        containerOptions={containerOptions}
        roleSelected={roleSelected}
        selectedNames={selectedNames}
        selectedContainers={selectedContainers}
        onFilterSelect={onFilterSelect}
        onNameSelect={onNameSelect}
        onNameClear={onNameClear}
        onContainerSelect={onContainerSelect}
        onContainerClear={onContainerClear}
        onRoleSelect={onRoleSelect}
        onSearch={onSearch}
        onDelete={onDelete}
        onChangeNameInput={onChangeNameInput}
        onChangeContainerInput={onChangeContainerInput}
        setNameInput={setNameInput}
        setContainerInput={setContainerInput}
      />
      <ToolbarItem data-codemods="true">
        {width < 769 && (
          <SortForMobileView
            sortMenu={sortMenuItems}
            sortValue={sortValue}
            setSortValue={setSortValue}
          />
        )}
      </ToolbarItem>
    </>
  );

  return (
    <Toolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onClearAllFilters}
      data-codemods="true"
    >
      <ToolbarContent>{toolbarItems}</ToolbarContent>
    </Toolbar>
  );
};
export { AddressLinksToolbar };
