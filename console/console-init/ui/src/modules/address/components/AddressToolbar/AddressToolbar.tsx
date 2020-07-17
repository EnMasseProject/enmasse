/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  ToolbarItem,
  Toolbar,
  ToolbarContent,
  ToolbarContentProps
} from "@patternfly/react-core";
import { ISortBy } from "@patternfly/react-table";
import { SortForMobileView, useWindowDimensions } from "components";
import {
  AddressListKebab,
  AddressToggleGroup,
  IAddressToggleGroupProps
} from "modules/address/components";

export interface IAddressToolbarProps extends IAddressToggleGroupProps {
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onClearAllFilters: () => void;
  onDeleteAllAddress: () => void;
  onPurgeAllAddress: () => void;
  isDeleteAllDisabled: boolean;
  isPurgeAllDisabled: boolean;
  onClickCreateAddress: () => void;
  namespace: string;
}

const AddressToolbar: React.FunctionComponent<IAddressToolbarProps &
  ToolbarContentProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  typeSelected,
  statusSelected,
  selectedNames,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onTypeSelect,
  onStatusSelect,
  onSearch,
  onDelete,
  sortValue,
  setSortValue,
  onClearAllFilters,
  onDeleteAllAddress,
  onPurgeAllAddress,
  isDeleteAllDisabled,
  isPurgeAllDisabled,
  onClickCreateAddress,
  onChangeNameInput,
  setNameInput
}) => {
  const { width } = useWindowDimensions();
  const sortMenuItems = [
    { key: "name", value: "Address", index: 1 },
    { key: "creationTimestamp", value: "Time Created", index: 4 },
    { key: "messageIn", value: "Message In", index: 5 },
    { key: "messageOut", value: "Message Out", index: 6 },
    { key: "storedMessage", value: "Stored Messages", index: 7 },
    { key: "senders", value: "Senders", index: 8 },
    { key: "receivers", value: "Receivers", index: 9 }
  ];
  const toolbarItems = (
    <>
      <AddressToggleGroup
        totalRecords={totalRecords}
        filterSelected={filterSelected}
        nameSelected={nameSelected}
        nameInput={nameInput}
        typeSelected={typeSelected}
        statusSelected={statusSelected}
        selectedNames={selectedNames}
        onFilterSelect={onFilterSelect}
        onNameSelect={onNameSelect}
        onNameClear={onNameClear}
        onTypeSelect={onTypeSelect}
        onStatusSelect={onStatusSelect}
        onSearch={onSearch}
        onDelete={onDelete}
        onChangeNameInput={onChangeNameInput}
        setNameInput={setNameInput}
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
      <ToolbarItem data-codemods="true" id="addr-toolbar-kebab">
        <AddressListKebab
          createAddressOnClick={onClickCreateAddress}
          onDeleteAllAddress={onDeleteAllAddress}
          onPurgeAllAddress={onPurgeAllAddress}
          isDeleteAllDisabled={isDeleteAllDisabled}
          isPurgeAllDisabled={isPurgeAllDisabled}
        />
      </ToolbarItem>
    </>
  );

  return (
    <Toolbar
      id="addr-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onClearAllFilters}
      data-codemods="true"
    >
      <ToolbarContent>{toolbarItems}</ToolbarContent>
    </Toolbar>
  );
};
export { AddressToolbar };
