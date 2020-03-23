/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";

import {
  DataToolbarChip,
  DataToolbarToggleGroup,
  DataToolbarGroup,
  DataToolbarFilter,
  DataToolbarItem
} from "@patternfly/react-core/dist/js/experimental";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import {
  InputGroup,
  Button,
  ButtonVariant,
  Badge,
  SelectOption,
  Select,
  SelectVariant,
  SelectOptionObject,
  DropdownPosition
} from "@patternfly/react-core";
import { ISelectOption } from "utils";
import {
  TypeAheadMessage,
  TYPEAHEAD_REQUIRED_LENGTH
} from "constants/constants";
import { DropdownWithToggle } from "components";

export interface IAddressSpaceFilterProps {
  onFilterSelect: (value: string) => void;
  onDelete: (
    type: string | DataToolbarChip,
    id: string | DataToolbarChip
  ) => void;
  onNameSelectToggle: () => void;
  onNameSelect: (event: any, selection: string | SelectOptionObject) => void;
  setNameSelected: (name: string | undefined) => void;
  setIsSelectNameExpanded: (expanded: boolean) => void;
  onNameSelectFilterChange: (
    event: React.ChangeEvent<HTMLInputElement>
  ) => React.ReactElement[];
  onClickSearchIcon: (event: any) => void;
  onNamespaceSelectToggle: () => void;
  onNamespaceSelect: (
    event: any,
    selection: string | SelectOptionObject
  ) => void;
  setNamespaceSelected: (name: string | undefined) => void;
  setIsSelectNamespaceExpanded: (expanded: boolean) => void;
  onTypeFilterSelect: (event: any) => void;
  onNamespaceSelectFilterChange: (
    e: React.ChangeEvent<HTMLInputElement>
  ) => React.ReactElement[];
  checkIsFilterApplied: () => boolean;
  filterValue?: string;
  filterNames: any[];
  filterNamespaces: any[];
  filterType?: string | null;
  totalAddressSpaces: number;
  namespaceSelected: string | undefined;
  isSelectNamespaceExpanded: boolean;
  namespaceOptions: Array<ISelectOption> | undefined;
  nameSpaceInput: string;
  typeFilterMenuItems: any[];
  filterMenuItems: any[];
  isSelectNameExpanded: boolean;
  nameOptions: Array<ISelectOption> | undefined;
  nameInput: string;
  nameSelected: string | undefined;
}

export const AddressSpaceFilter: React.FC<IAddressSpaceFilterProps> = ({
  onFilterSelect,
  onDelete,
  onNameSelectToggle,
  onNameSelect,
  setNameSelected,
  setIsSelectNameExpanded,
  onNameSelectFilterChange,
  onClickSearchIcon,
  onNamespaceSelectToggle,
  onNamespaceSelect,
  setNamespaceSelected,
  setIsSelectNamespaceExpanded,
  onTypeFilterSelect,
  onNamespaceSelectFilterChange,
  checkIsFilterApplied,
  filterValue,
  filterNames,
  filterNamespaces,
  filterType,
  totalAddressSpaces,
  namespaceSelected,
  isSelectNamespaceExpanded,
  namespaceOptions,
  nameSpaceInput,
  typeFilterMenuItems,
  filterMenuItems,
  isSelectNameExpanded,
  nameOptions,
  nameInput,
  nameSelected
}) => {
  const toggleGroupItems = (
    <>
      <DataToolbarGroup variant="filter-group">
        <DataToolbarFilter categoryName="Filter">
          <DropdownWithToggle
            id="al-filter-dropdown"
            toggleId={"al-filter-dropdown"}
            position={DropdownPosition.left}
            onSelectItem={onFilterSelect}
            dropdownItems={filterMenuItems}
            value={filterValue?.trim() || "Filter"}
            toggleIcon={
              <>
                <FilterIcon />
                &nbsp;
              </>
            }
          />
        </DataToolbarFilter>
        {filterValue && filterValue.trim() !== "" && (
          <>
            <DataToolbarItem>
              <DataToolbarFilter
                chips={filterNames.map(filter => filter.value)}
                deleteChip={onDelete}
                categoryName="Name"
              >
                {filterValue && filterValue === "Name" && (
                  <InputGroup>
                    <Select
                      id="al-filter-input-name"
                      variant={SelectVariant.typeahead}
                      aria-label="Select a Name"
                      onToggle={onNameSelectToggle}
                      onSelect={onNameSelect}
                      onClear={() => {
                        setNameSelected(undefined);
                        setIsSelectNameExpanded(false);
                      }}
                      maxHeight="200px"
                      selections={nameSelected}
                      onFilter={onNameSelectFilterChange}
                      isExpanded={isSelectNameExpanded}
                      ariaLabelledBy={"typeahead-select-id"}
                      placeholderText="Select name"
                      isDisabled={false}
                      isCreatable={false}
                    >
                      {nameOptions && nameOptions.length > 0 ? (
                        nameOptions.map((option: any, index: number) => (
                          <SelectOption
                            key={index}
                            value={option.value}
                            isDisabled={option.isDisabled}
                          />
                        ))
                      ) : nameInput.trim().length <
                        TYPEAHEAD_REQUIRED_LENGTH ? (
                        <SelectOption
                          key={"invalid-input-length"}
                          value={TypeAheadMessage.MORE_CHAR_REQUIRED}
                          disabled={true}
                        />
                      ) : (
                        <SelectOption
                          key={"no-results-found"}
                          value={TypeAheadMessage.NO_RESULT_FOUND}
                          disabled={true}
                        />
                      )}
                      {/* {} */}
                    </Select>
                    <Button
                      id="al-filter-search-name"
                      variant={ButtonVariant.control}
                      aria-label="search button for search input"
                      onClick={onClickSearchIcon}
                    >
                      <SearchIcon />
                    </Button>
                  </InputGroup>
                )}
              </DataToolbarFilter>
            </DataToolbarItem>
            <DataToolbarItem>
              <DataToolbarFilter
                chips={filterNamespaces.map(filter => filter.value)}
                deleteChip={onDelete}
                categoryName="Namespace"
              >
                {filterValue && filterValue === "Namespace" && (
                  <InputGroup>
                    <Select
                      id="al-filter-input-namespace"
                      variant={SelectVariant.typeahead}
                      aria-label="Select a Namespace"
                      onToggle={onNamespaceSelectToggle}
                      onSelect={onNamespaceSelect}
                      onClear={() => {
                        setNamespaceSelected(undefined);
                        setIsSelectNamespaceExpanded(false);
                      }}
                      maxHeight="200px"
                      selections={namespaceSelected}
                      onFilter={onNamespaceSelectFilterChange}
                      isExpanded={isSelectNamespaceExpanded}
                      ariaLabelledBy={"typeahead-select-id"}
                      placeholderText="Select Namespace"
                      isDisabled={false}
                      isCreatable={false}
                    >
                      {namespaceOptions && namespaceOptions.length > 0 ? (
                        namespaceOptions.map((option: any, index: number) => (
                          <SelectOption
                            key={index}
                            value={option.value}
                            isDisabled={option.isDisabled}
                          />
                        ))
                      ) : nameSpaceInput.trim().length <
                        TYPEAHEAD_REQUIRED_LENGTH ? (
                        <SelectOption
                          key={"invalid-input-length"}
                          value={TypeAheadMessage.MORE_CHAR_REQUIRED}
                          disabled={true}
                        />
                      ) : (
                        <SelectOption
                          key={"no-results-found"}
                          value={TypeAheadMessage.NO_RESULT_FOUND}
                          disabled={true}
                        />
                      )}
                      {/* {} */}
                    </Select>
                    <Button
                      id="al-filter-search-namespace"
                      variant={ButtonVariant.control}
                      aria-label="search button for search input"
                      onClick={onClickSearchIcon}
                    >
                      <SearchIcon />
                    </Button>
                  </InputGroup>
                )}
              </DataToolbarFilter>
            </DataToolbarItem>
            <DataToolbarItem>
              <DataToolbarFilter
                chips={filterType ? [filterType] : []}
                deleteChip={onDelete}
                categoryName="Type"
              >
                {filterValue && filterValue === "Type" && (
                  <InputGroup>
                    <DropdownWithToggle
                      id="al-filter-dropdown-type"
                      position="left"
                      onSelectItem={onTypeFilterSelect}
                      dropdownItems={typeFilterMenuItems}
                      value={(filterType && filterType.trim()) || "Select Type"}
                    />
                  </InputGroup>
                )}
              </DataToolbarFilter>
            </DataToolbarItem>
          </>
        )}
      </DataToolbarGroup>
    </>
  );

  return (
    <DataToolbarToggleGroup
      toggleIcon={
        <>
          <FilterIcon />
          {checkIsFilterApplied() && (
            <Badge key={1} isRead>
              {totalAddressSpaces}
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
