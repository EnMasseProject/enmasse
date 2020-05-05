/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  SelectVariant,
  SelectOptionObject,
  DataToolbarToggleGroup,
  DataToolbarGroup,
  DataToolbarFilter,
  InputGroup,
  Button,
  DataToolbarItem,
  ButtonVariant,
  DataToolbarChip,
  DataToolbarChipGroup,
  DropdownPosition,
  Badge
} from "@patternfly/react-core";
import { ISelectOption } from "utils";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import {
  TypeAheadSelect,
  DropdownWithToggle,
  SelectWithToggle
} from "components";

export interface IAddressToggleGroupProps {
  totalRecords: number;
  filterSelected?: string;
  nameSelected?: string;
  nameInput?: string;
  typeSelected?: string | null;
  statusSelected?: string | null;
  selectedNames: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onNameSelect: (e: any, selection: SelectOptionObject) => void;
  onNameClear: () => void;
  onTypeSelect: (selection: string) => void;
  onStatusSelect: (selection: string) => void;
  onSearch: () => void;
  onDelete: (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => void;
  onChangeNameInput?: (value: string) => Promise<any>;
  setNameInput?: (value: string) => void;
}

const AddressToggleGroup: React.FunctionComponent<IAddressToggleGroupProps> = ({
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
  onChangeNameInput,
  setNameInput
}) => {
  const filterMenuItems = [
    { key: "name", value: "Name" },
    { key: "type", value: "Type" },
    { key: "status", value: "Status" }
  ];
  const typeOptions: ISelectOption[] = [
    { key: "anycast", value: "Anycast", isDisabled: false },
    { key: "multicast", value: "Multicast", isDisabled: false },
    { key: "queue", value: "Queue", isDisabled: false },
    { key: "subscription", value: "Subscription", isDisabled: false },
    { key: "topic", value: "Topic", isDisabled: false }
  ];

  const statusOptions: ISelectOption[] = [
    { key: "active", value: "Active", isDisabled: false },
    { key: "configuring", value: "Configuring", isDisabled: false },
    { key: "pending", value: "Pending", isDisabled: false },
    { key: "failed", value: "Failed", isDisabled: false }
  ];

  const checkIsFilterApplied = () => {
    if (
      (selectedNames && selectedNames.length > 0) ||
      (typeSelected && typeSelected.trim() !== "") ||
      (statusSelected && statusSelected.trim() !== "")
    ) {
      return true;
    }
    return false;
  };
  const toggleItems = (
    <>
      <DataToolbarItem
        breakpointMods={[{ modifier: "spacer-none", breakpoint: "md" }]}
      >
        <DataToolbarFilter
          chips={selectedNames.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Name"
        >
          {filterSelected && filterSelected.toLowerCase() === "name" && (
            <InputGroup>
              <TypeAheadSelect
                ariaLabelTypeAhead={"Select name"}
                ariaLabelledBy={"typeahead-select-id"}
                onSelect={onNameSelect}
                onClear={onNameClear}
                selected={nameSelected}
                inputData={nameInput || ""}
                placeholderText={"Select name"}
                onChangeInput={onChangeNameInput}
                setInput={setNameInput}
              />
              <Button
                id="al-filter-search-name"
                variant={ButtonVariant.control}
                aria-label="search button for search name"
                onClick={onSearch}
              >
                <SearchIcon />
              </Button>
            </InputGroup>
          )}
        </DataToolbarFilter>
      </DataToolbarItem>
      <DataToolbarItem
        breakpointMods={[{ modifier: "spacer-none", breakpoint: "md" }]}
      >
        <DataToolbarFilter
          chips={typeSelected ? [typeSelected] : []}
          deleteChip={onDelete}
          categoryName="Type"
        >
          {filterSelected && filterSelected.toLowerCase() === "type" && (
            <DropdownWithToggle
              id="al-filter-select-type-dropdown"
              dropdownItemId="al-filter-select-type-dropdown-item"
              position={DropdownPosition.left}
              onSelectItem={onTypeSelect}
              dropdownItems={typeOptions}
              value={typeSelected || "Select Type"}
            />
          )}
        </DataToolbarFilter>
      </DataToolbarItem>
      <DataToolbarItem
        breakpointMods={[{ modifier: "spacer-none", breakpoint: "md" }]}
      >
        <DataToolbarFilter
          chips={statusSelected ? [statusSelected] : []}
          deleteChip={onDelete}
          categoryName="Status"
        >
          {filterSelected && filterSelected.toLowerCase() === "status" && (
            <DropdownWithToggle
              id="al-filter-select-status-dropdown"
              dropdownItemId="al-filter-select-status-dropdown-item"
              position={DropdownPosition.left}
              onSelectItem={onStatusSelect}
              dropdownItems={statusOptions}
              value={statusSelected || "Select Status"}
            />
          )}
        </DataToolbarFilter>
      </DataToolbarItem>
    </>
  );

  const toggleGroupItems = (
    <DataToolbarGroup variant="filter-group">
      <DataToolbarFilter categoryName="Filter">
        <DropdownWithToggle
          id="al-filter-dropdown"
          toggleId={"al-filter-dropdown"}
          dropdownItemId="al-filter-dropdown"
          position={DropdownPosition.left}
          onSelectItem={onFilterSelect}
          dropdownItems={filterMenuItems}
          value={(filterSelected && filterSelected.trim()) || "Filter"}
          toggleIcon={
            <>
              <FilterIcon />
              &nbsp;
            </>
          }
        />
        {toggleItems}
      </DataToolbarFilter>
    </DataToolbarGroup>
  );

  return (
    <DataToolbarToggleGroup
      toggleIcon={
        <>
          <FilterIcon />
          {checkIsFilterApplied() && (
            <Badge key={1} isRead>
              {totalRecords}
            </Badge>
          )}
        </>
      }
      breakpoint="xl"
    >
      {toggleGroupItems}
    </DataToolbarToggleGroup>
  );
};
export { AddressToggleGroup };
