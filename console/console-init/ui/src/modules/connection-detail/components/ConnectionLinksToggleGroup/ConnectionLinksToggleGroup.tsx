/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  SelectVariant,
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
import {
  DropdownWithToggle,
  TypeAheadSelect,
  SelectWithToggle
} from "components";

export interface IConnectionLinksToggleGroupProps {
  totalRecords: number;
  filterSelected?: string;
  nameSelected?: string;
  nameInput?: string;
  addressSelected?: string;
  addressInput?: string;
  roleSelected?: string | null;
  selectedNames: Array<{ value: string; isExact: boolean }>;
  selectedAddresses: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onNameSelect: (e: any, selection: SelectOptionObject) => void;
  onNameClear: () => void;
  onAddressSelect: (e: any, selection: SelectOptionObject) => void;
  onAddressClear: () => void;
  onRoleSelect: (selection: string) => void;
  onSearch: () => void;
  onDelete: (
    category: string | ToolbarChipGroup,
    chip: string | ToolbarChip
  ) => void;
  onChangeNameInput?: (value: string) => Promise<any>;
  onChangeAddressInput?: (value: string) => Promise<any>;
  setNameInput?: (value: string) => void;
  setAddressInput?: (value: string) => void;
}
const ConnectionLinksToggleGroup: React.FunctionComponent<IConnectionLinksToggleGroupProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  addressSelected,
  addressInput,
  roleSelected,
  selectedNames,
  selectedAddresses,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onAddressSelect,
  onAddressClear,
  onRoleSelect,
  onSearch,
  onDelete,
  onChangeNameInput,
  onChangeAddressInput,
  setNameInput,
  setAddressInput
}) => {
  const filterMenuItems = [
    { key: "name", value: "Name" },
    { key: "address", value: "Address" },
    { key: "role", value: "Role" }
  ];
  const roleOptions: ISelectOption[] = [
    { key: "sender", value: "Sender", isDisabled: false },
    { key: "receiver", value: "Receiver", isDisabled: false }
  ];

  const checkIsFilterApplied = () => {
    if (
      (selectedNames && selectedNames.length > 0) ||
      (selectedAddresses && selectedAddresses.length > 0) ||
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
          id="connection-links-togglegrp-name-filter"
          chips={selectedNames.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Name"
        >
          {filterSelected && filterSelected.toLowerCase() === "name" && (
            <InputGroup>
              <TypeAheadSelect
                id="connection-links-togglegrp-name-typeahead"
                aria-label="select name"
                typeAheadAriaLabel={"Select name"}
                aria-LabelledBy={"typeahead-select-id"}
                onSelect={onNameSelect}
                onClear={onNameClear}
                selected={nameSelected}
                inputData={nameInput || ""}
                placeholderText={"Select name"}
                onChangeInput={onChangeNameInput}
                setInput={setNameInput}
              />
              <Button
                id="connection-links-togglegrp-search-name-button"
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
          id="connection-links-togglegrp-address-filter"
          chips={selectedAddresses.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Address"
        >
          {filterSelected && filterSelected === "Address" && (
            <InputGroup>
              <TypeAheadSelect
                id="connection-links-togglegrp-address-typeahead"
                aria-label="select address"
                typeAheadAriaLabel={"Select address"}
                aria-LabelledBy={"typeahead-select-id"}
                onSelect={onAddressSelect}
                onClear={onAddressClear}
                selected={addressSelected}
                inputData={addressInput || ""}
                placeholderText={"Select address"}
                onChangeInput={onChangeAddressInput}
                setInput={setAddressInput}
              />
              <Button
                id="connection-links-togglegrp-search-address-button"
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
          id="connection-links-togglegrp-role-filter"
          chips={roleSelected ? [roleSelected] : []}
          deleteChip={onDelete}
          categoryName="Role"
        >
          {filterSelected === "Role" && (
            <SelectWithToggle
              id="connection-links-togglegrp-role-selectwithtoggle"
              variant={SelectVariant.single}
              ariaLabel="Select Role"
              onSelectItem={onRoleSelect}
              selections={roleSelected || "Select Role"}
              selectOptions={roleOptions}
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
          id="connection-links-togglegrp-filter-dropdowntoggle"
          toggleId="connectionlink-filter-dropdown-toggle"
          dropdownItemIdPrefix="cl-filter-dropdown"
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
            <Badge
              id="connection-links-togglegrp-total-records-badge"
              key={1}
              isRead
            >
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
export { ConnectionLinksToggleGroup };
