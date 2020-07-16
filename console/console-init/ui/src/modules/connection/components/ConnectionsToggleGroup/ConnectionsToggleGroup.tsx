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
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import { TypeAheadSelect, DropdownWithToggle } from "components";

export interface IConnectionsToggleGroupProps {
  totalRecords: number;
  filterSelected?: string;
  hostnameSelected?: string;
  hostnameInput?: string;
  containerSelected?: string;
  containerInput?: string;
  selectedHostnames: Array<{ value: string; isExact: boolean }>;
  selectedContainers: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onHostnameSelect: (e: any, selection: SelectOptionObject) => void;
  onHostnameClear: () => void;
  onContainerSelect: (e: any, selection: SelectOptionObject) => void;
  onContainerClear: () => void;
  onSearch: () => void;
  onDelete: (
    category: string | ToolbarChipGroup,
    chip: string | ToolbarChip
  ) => void;
  onChangeHostNameInput?: (value: string) => Promise<any>;
  onChangeContainerInput?: (value: string) => Promise<any>;
  setHostNameInput?: (value: string) => void;
  setHostContainerInput?: (value: string) => void;
}
const ConnectionsToggleGroup: React.FunctionComponent<IConnectionsToggleGroupProps> = ({
  totalRecords,
  filterSelected,
  hostnameSelected,
  hostnameInput,
  containerSelected,
  containerInput,
  selectedHostnames,
  selectedContainers,
  onFilterSelect,
  onHostnameSelect,
  onHostnameClear,
  onContainerSelect,
  onContainerClear,
  onSearch,
  onDelete,
  onChangeHostNameInput,
  onChangeContainerInput,
  setHostNameInput,
  setHostContainerInput
}) => {
  const filterMenuItems = [
    { key: "hostname", value: "Hostname" },
    { key: "container", value: "Container" }
  ];

  const checkIsFilterApplied = () => {
    if (
      (selectedHostnames && selectedHostnames.length > 0) ||
      (selectedContainers && selectedContainers.length > 0)
    ) {
      return true;
    }
    return false;
  };
  const toggleItems = (
    <>
      <ToolbarItem spacer={{ md: "spacerNone" }} data-codemods="true">
        <ToolbarFilter
          chips={selectedHostnames.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Hostname"
        >
          {filterSelected && filterSelected.toLowerCase() === "hostname" && (
            <InputGroup>
              <TypeAheadSelect
                id="connections-toggle-grp-select-hostname-typeahead"
                aria-label="Select hostname"
                typeAheadAriaLabel={"Select hostname"}
                aria-LabelledBy={"typeahead-select-id"}
                onSelect={onHostnameSelect}
                onClear={onHostnameClear}
                selected={hostnameSelected}
                inputData={hostnameInput || ""}
                placeholderText={"Select hostname"}
                onChangeInput={onChangeHostNameInput}
                setInput={setHostNameInput}
              />
              <Button
                id="connections-togglegrp-search-hostname-button"
                variant={ButtonVariant.control}
                aria-label="search button for search hostname"
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
                id="connections-togglegrp-select-container-typeahead"
                aria-label="Select container"
                typeAheadAriaLabel={"Select container"}
                aria-LabelledBy={"typeahead-select-id"}
                onSelect={onContainerSelect}
                onClear={onContainerClear}
                selected={containerSelected}
                inputData={containerInput || ""}
                placeholderText={"Select container"}
                onChangeInput={onChangeContainerInput}
                setInput={setHostContainerInput}
              />
              <Button
                id="connections-togglegrp-search-addr-button"
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
    </>
  );

  const toggleGroupItems = (
    <ToolbarGroup variant="filter-group" data-codemods="true">
      <ToolbarFilter categoryName="Filter">
        <DropdownWithToggle
          id="connections-togglegrp-filter-dropdown"
          toggleId={"connections-togglegrp-filter-dropdown-toggle"}
          dropdownItemIdPrefix="cl-filter-dropdown-item"
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
export { ConnectionsToggleGroup };
