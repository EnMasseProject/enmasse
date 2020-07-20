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
import {
  DropdownWithToggle,
  TypeAheadSelect,
  DropdownWithBulkSelect
} from "components";
import { typeOptions, filterMenuItems } from "modules/project/utils";

export interface IProjectToolbarToggleGroupProps {
  totalRecords: number;
  filterSelected?: string;
  nameSelected?: string;
  nameInput?: string;
  namespaceSelected?: string;
  namespaceInput?: string;
  nameOptions?: any[];
  namespaceOptions?: any[];
  typeSelected?: string | null;
  selectedNames: Array<{ value: string; isExact: boolean }>;
  selectedNamespaces: Array<{ value: string; isExact: boolean }>;
  onSelectFilter: (value: string) => void;
  onSelectName: (e: any, selection: SelectOptionObject) => void;
  onClearName: () => void;
  onSelectNamespace: (e: any, selection: SelectOptionObject) => void;
  onClearNamespace: () => void;
  onSelectType: (selection: string) => void;
  onDeleteAll: () => void;
  onSearch: () => void;
  onDelete: (
    category: string | ToolbarChipGroup,
    chip: string | ToolbarChip
  ) => void;
  onChangeNameInput?: (value: string) => Promise<any>;
  onChangeNameSpaceInput?: (value: string) => Promise<any>;
  setNameInput?: (value: string) => void;
  setNameSpaceInput?: (value: string) => void;
  isAllProjectSelected: boolean;
  onSelectAllProjects: (val: boolean) => void;
}
const ProjectToolbarToggleGroup: React.FunctionComponent<IProjectToolbarToggleGroupProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  namespaceSelected,
  namespaceInput,
  typeSelected,
  selectedNames,
  selectedNamespaces,
  onSelectFilter,
  onSelectName,
  onClearName,
  onSelectNamespace,
  onClearNamespace,
  onSelectType,
  onSearch,
  onDelete,
  onChangeNameInput,
  onChangeNameSpaceInput,
  setNameInput,
  setNameSpaceInput,
  isAllProjectSelected,
  onSelectAllProjects
}) => {
  const isFilterApplied = () => {
    if (
      (selectedNames && selectedNames.length > 0) ||
      (selectedNamespaces && selectedNamespaces.length > 0) ||
      (typeSelected && typeSelected.trim() !== "")
    ) {
      return true;
    }
    return false;
  };
  const onSelectAll = (val: boolean) => {
    onSelectAllProjects(val);
  };
  const toggleItems = (
    <>
      <ToolbarItem spacer={{ md: "spacerNone" }} data-codemods="true">
        <ToolbarFilter
          id="project-data-togglegrp-name-filter"
          chips={selectedNames.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Name"
        >
          {filterSelected && filterSelected.toLowerCase() === "name" && (
            <InputGroup>
              <TypeAheadSelect
                id="project-data-togglegrp-input-name-typeahead"
                typeAheadAriaLabel={"Select name"}
                aria-LabelledBy={"typeahead-select-id"}
                onSelect={onSelectName}
                onClear={onClearName}
                selected={nameSelected}
                inputData={nameInput || ""}
                placeholderText={"Select name"}
                onChangeInput={onChangeNameInput}
                setInput={setNameInput}
              />
              <Button
                id="project-data-togglegrp-search-name-button"
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
          id="project-data-togglegrp-namespace-filter"
          chips={selectedNamespaces.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Namespace"
        >
          {filterSelected && filterSelected.toLowerCase() === "namespace" && (
            <InputGroup>
              <TypeAheadSelect
                id="project-data-togglegrp-input-namespace-typeahead"
                typeAheadAriaLabel={"Select namespace"}
                aria-LabelledBy={"typeahead-select-id"}
                onSelect={onSelectNamespace}
                onClear={onClearNamespace}
                selected={namespaceSelected}
                inputData={namespaceInput || ""}
                placeholderText={"Select namespace"}
                onChangeInput={onChangeNameSpaceInput}
                setInput={setNameSpaceInput}
              />
              <Button
                id="project-data-togglegrp-search-namespace-button"
                variant={ButtonVariant.control}
                aria-label="search button for search namespace"
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
          id="project-data-togglegrp-type-filter"
          chips={typeSelected ? [typeSelected] : []}
          deleteChip={onDelete}
          categoryName="Type"
        >
          {filterSelected && filterSelected.toLowerCase() === "type" && (
            <DropdownWithToggle
              id={"project-data-togglegrp-type-dropdowntoggle"}
              toggleId={"project-data-togglegrp-type-dropdown-toggle"}
              dropdownItemIdPrefix={"al-filter-dropdown-item-type"}
              position={DropdownPosition.left}
              onSelectItem={onSelectType}
              dropdownItems={typeOptions}
              value={typeSelected || "Select Type"}
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
          id="project-data-togglegrp-filter-menu-dropdowntoggle"
          toggleId={"project-data-togglegrp-filter-menu-dropdown-toggle"}
          position={DropdownPosition.left}
          onSelectItem={onSelectFilter}
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
    <>
      <ToolbarToggleGroup
        toggleIcon={
          <>
            <FilterIcon />
            {isFilterApplied() && (
              <Badge
                id="project-data-togglegrp-total-records-badge"
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
        <ToolbarItem
          variant="bulk-select"
          id="project-data-togglegrp-device-list-toolbaritem"
          key="bulk-select"
          aria-label="Select multiple devices"
          data-codemods="true"
        >
          <DropdownWithBulkSelect
            dropdownId="project-data-togglegrp-device-bulk-select-dropdown"
            dropdownToggleId="device-bulk-select-toggle"
            checkBoxId="device-bulk-select-checkbox"
            ariaLabel="Bulk select dropdown for device list"
            isChecked={isAllProjectSelected}
            onChange={onSelectAll}
          />
        </ToolbarItem>
        {toggleGroupItems}
      </ToolbarToggleGroup>
    </>
  );
};
export { ProjectToolbarToggleGroup };
