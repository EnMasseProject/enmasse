/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Select,
  SelectVariant,
  SelectOptionObject,
  SelectOption,
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
import { TypeAheadSelect, DropdownWithToggle } from "components";

export interface IAddressToggleGroupProps {
  totalRecords: number;
  filterSelected?: string;
  nameSelected?: string;
  nameInput?: string;
  typeIsExpanded: boolean;
  typeSelected?: string | null;
  statusIsExpanded: boolean;
  statusSelected?: string | null;
  selectedNames: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onNameSelect: (e: any, selection: SelectOptionObject) => void;
  onNameClear: () => void;
  onTypeToggle: () => void;
  onTypeSelect: (e: any, selection: SelectOptionObject) => void;
  onStatusToggle: () => void;
  onStatusSelect: (e: any, selection: SelectOptionObject) => void;
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
  typeIsExpanded,
  typeSelected,
  statusIsExpanded,
  statusSelected,
  selectedNames,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onTypeToggle,
  onTypeSelect,
  onStatusToggle,
  onStatusSelect,
  onSearch,
  onDelete,
  onChangeNameInput,
  setNameInput
}) => {
  const filterMenuItems = [
    { key: "filterName", value: "Name" },
    { key: "filterType", value: "Type" },
    { key: "filterStatus", value: "Status" }
  ];
  const typeOptions: ISelectOption[] = [
    { value: "Anycast", isDisabled: false },
    { value: "Multicast", isDisabled: false },
    { value: "Queue", isDisabled: false },
    { value: "Subscription", isDisabled: false },
    { value: "Topic", isDisabled: false }
  ];

  const statusOptions: ISelectOption[] = [
    { value: "Active", isDisabled: false },
    { value: "Configuring", isDisabled: false },
    { value: "Pending", isDisabled: false }
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
                id="ad-links-filter-search-name"
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
            <Select
              variant={SelectVariant.single}
              aria-label="Select Type"
              onToggle={onTypeToggle}
              onSelect={onTypeSelect}
              selections={typeSelected || "Select Type"}
              isExpanded={typeIsExpanded}
            >
              {typeOptions.map((option, index) => (
                <SelectOption
                  isDisabled={option.isDisabled}
                  key={index}
                  value={option.value}
                />
              ))}
            </Select>
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
            <Select
              variant={SelectVariant.single}
              aria-label="Select Status"
              onToggle={onStatusToggle}
              onSelect={onStatusSelect}
              selections={statusSelected || "Select Status"}
              isExpanded={statusIsExpanded}
            >
              {statusOptions.map((option, index) => (
                <SelectOption
                  isDisabled={option.isDisabled}
                  key={index}
                  value={option.value}
                />
              ))}
            </Select>
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
