/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  MessagingToolbarToggleGroup,
  IMessagingToolbarToggleGroupProps
} from "modules/address-space/components";
import { SortForMobileView, useWindowDimensions } from "components";
import {
  DataToolbarItem,
  DataToolbar,
  DataToolbarContent,
  DataToolbarContentProps
} from "@patternfly/react-core";
import { AddressSpaceListKebab } from "modules/address-space/components";
import { ISortBy } from "@patternfly/react-table";
export interface IMessageToolbarProps
  extends IMessagingToolbarToggleGroupProps {
  onNamespaceClear: () => void;
  onCreateAddressSpace: () => void;
  isDeleteAllDisabled: boolean;
  onSelectDeleteAll: (event: any) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onClearAllFilters: () => void;
}
const MessagingToolbar: React.FunctionComponent<IMessageToolbarProps &
  DataToolbarContentProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  namespaceSelected,
  namespaceInput,
  nameOptions,
  namespaceOptions,
  typeSelected,
  statusSelected,
  selectedNames,
  selectedNamespaces,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onNamespaceSelect,
  onNamespaceClear,
  onTypeSelect,
  onStatusSelect,
  onDeleteAll,
  onSearch,
  onDelete,
  onCreateAddressSpace,
  isDeleteAllDisabled,
  onSelectDeleteAll,
  sortValue,
  setSortValue,
  onClearAllFilters,
  onChangeNameInput,
  onChangeNameSpaceInput,
  setNameInput,
  setNameSpaceInput
}) => {
  const { width } = useWindowDimensions();
  const sortMenuItems = [
    { key: "name", value: "Name", index: 1 },
    { key: "creationTimestamp", value: "Time Created", index: 4 }
  ];
  const toolbarItems = (
    <>
      <MessagingToolbarToggleGroup
        totalRecords={totalRecords}
        filterSelected={filterSelected}
        nameSelected={nameSelected}
        nameInput={nameInput}
        namespaceSelected={namespaceSelected}
        namespaceInput={namespaceInput}
        nameOptions={nameOptions}
        namespaceOptions={namespaceOptions}
        typeSelected={typeSelected}
        statusSelected={statusSelected}
        selectedNames={selectedNames}
        selectedNamespaces={selectedNamespaces}
        onFilterSelect={onFilterSelect}
        onNameSelect={onNameSelect}
        onNameClear={onNameClear}
        onNamespaceSelect={onNamespaceSelect}
        onNamespaceClear={onNamespaceClear}
        onTypeSelect={onTypeSelect}
        onStatusSelect={onStatusSelect}
        onDeleteAll={onDeleteAll}
        onSearch={onSearch}
        onDelete={onDelete}
        onChangeNameInput={onChangeNameInput}
        onChangeNameSpaceInput={onChangeNameSpaceInput}
        setNameInput={setNameInput}
        setNameSpaceInput={setNameSpaceInput}
      />
      <DataToolbarItem>
        {width < 769 && (
          <SortForMobileView
            sortMenu={sortMenuItems}
            sortValue={sortValue}
            setSortValue={setSortValue}
          />
        )}
      </DataToolbarItem>
      <DataToolbarItem>
        <AddressSpaceListKebab
          onCreateAddressSpace={onCreateAddressSpace}
          onSelectDeleteAll={onSelectDeleteAll}
          isDeleteAllDisabled={isDeleteAllDisabled}
        />
      </DataToolbarItem>
    </>
  );

  return (
    <DataToolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onClearAllFilters}
    >
      <DataToolbarContent>{toolbarItems}</DataToolbarContent>
    </DataToolbar>
  );
};
export { MessagingToolbar };
