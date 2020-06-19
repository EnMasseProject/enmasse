/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  SelectOptionObject,
  ToolbarToggleGroup,
  ToolbarGroup,
  ToolbarFilter,
  InputGroup,
  Button,
  ToolbarItem,
  ButtonVariant,
  DropdownPosition,
  Badge,
  ToolbarChipGroup,
  ToolbarChip
} from "@patternfly/react-core";
import { ISelectOption } from "utils";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import { DropdownWithToggle, TypeAheadSelect } from "components";

export interface IAddressLinksToggleGroupProps {
  totalRecords: number;
  filterSelected?: string;
  nameSelected?: string;
  nameInput?: string;
  containerSelected?: string;
  containerInput?: string;
  containerOptions?: any[];
  roleSelected?: string | null;
  selectedNames: Array<{ value: string; isExact: boolean }>;
  selectedContainers: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onNameSelect: (e: any, selection: SelectOptionObject) => void;
  onNameClear: () => void;
  onContainerSelect: (e: any, selection: SelectOptionObject) => void;
  onContainerClear: () => void;
  onRoleSelect: (selection: string) => void;
  onSearch: () => void;
  onDelete: (
    category: string | ToolbarChipGroup,
    chip: string | ToolbarChip
  ) => void;
  onChangeNameInput?: (value: string) => Promise<any>;
  onChangeContainerInput?: (value: string) => Promise<any>;
  setNameInput?: (value: string) => void;
  setContainerInput?: (value: string) => void;
}
const AddressLinksToggleGroup: React.FunctionComponent<IAddressLinksToggleGroupProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  containerSelected,
  containerInput,
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
  onChangeNameInput,
  onChangeContainerInput,
  setNameInput,
  setContainerInput
}) => {
  const filterMenuItems = [
    { key: "name", value: "Name" },
    { key: "container", value: "Container" },
    { key: "role", value: "Role" }
  ];

  const roleOptions: ISelectOption[] = [
    { key: "sender", value: "Sender", isDisabled: false },
    { key: "receiver", value: "Receiver", isDisabled: false }
  ];

  const checkIsFilterApplied = () => {
    if (
      (selectedNames && selectedNames.length > 0) ||
      (selectedContainers && selectedContainers.length > 0) ||
      (roleSelected && roleSelected.trim() !== "")
    ) {
      return true;
    }
    return false;
  };
  const toggleItems = (
    <>
      <ToolbarItem spacer={{ md: "spacerNone" }} data-codemods="true">
        <ToolbarFilter
          chips={selectedNames.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Name"
        >
          {filterSelected && filterSelected.toLowerCase() === "name" && (
            <InputGroup>
              <TypeAheadSelect
                id="ad-links-filter-select-name"
                typeAheadAriaLabel={"Select name"}
                aria-labelledby={"typeahead-select-id"}
                onSelect={onNameSelect}
                onClear={onNameClear}
                selected={nameSelected}
                inputData={nameInput || ""}
                placeholderText={"Select name"}
                onChangeInput={onChangeNameInput}
                setInput={setNameInput}
              />
              <Button
                id="ad-links-filter-search-name"
                variant={ButtonVariant.control}
                aria-label="search button for search name"
                onClick={onSearch}
              >
                <SearchIcon />
              </Button>
            </InputGroup>
          )}
        </ToolbarFilter>
      </ToolbarItem>
      <ToolbarItem spacer={{ md: "spacerNone" }} data-codemods="true">
        <ToolbarFilter
          chips={selectedContainers.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Container"
        >
          {filterSelected && filterSelected.toLowerCase() === "container" && (
            <InputGroup>
              <TypeAheadSelect
                typeAheadAriaLabel={"Select container"}
                aria-labelledby={"typeahead-select-id"}
                onSelect={onContainerSelect}
                onClear={onContainerClear}
                selected={containerSelected}
                inputData={containerInput || ""}
                placeholderText={"Select container"}
                onChangeInput={onChangeContainerInput}
                setInput={setContainerInput}
              />
              <Button
                id="ad-links-filter-search-container"
                variant={ButtonVariant.control}
                aria-label="search button for search address"
                onClick={onSearch}
              >
                <SearchIcon />
              </Button>
            </InputGroup>
          )}
        </ToolbarFilter>
      </ToolbarItem>
      <ToolbarItem spacer={{ md: "spacerNone" }} data-codemods="true">
        <ToolbarFilter
          chips={roleSelected ? [roleSelected] : []}
          deleteChip={onDelete}
          categoryName="Role"
        >
          {filterSelected && filterSelected.toLowerCase() === "role" && (
            <DropdownWithToggle
              id="ad-links-filter-select-role"
              toggleId="ad-links-filter-select-role"
              dropdownItemIdPrefix="ad-links-filter-select-option-role"
              position={DropdownPosition.left}
              onSelectItem={onRoleSelect}
              dropdownItems={roleOptions}
              value={roleSelected || "Select Role"}
            />
          )}
        </ToolbarFilter>
      </ToolbarItem>
    </>
  );

  const toggleGroupItems = (
    <ToolbarGroup variant="filter-group" data-codemods="true">
      <ToolbarFilter categoryName="Filter">
        <DropdownWithToggle
          id="ad-links-filter-dropdown"
          dropdownItemIdPrefix={"ad-links-filter-dropdown-item"}
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
      </ToolbarFilter>
    </ToolbarGroup>
  );

  return (
    <ToolbarToggleGroup
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
    </ToolbarToggleGroup>
  );
};
export { AddressLinksToggleGroup };
