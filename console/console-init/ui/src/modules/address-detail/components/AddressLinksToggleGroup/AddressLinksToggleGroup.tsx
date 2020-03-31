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
import { DropdownWithToggle, TypeAhead } from "components";

export interface IAddressLinksToggleGroupProps {
  totalRecords: number;
  filterSelected?: string;
  nameSelected?: string;
  nameInput?: string;
  containerSelected?: string;
  containerInput?: string;
  nameOptions?: any[];
  containerOptions?: any[];
  roleIsExpanded: boolean;
  roleSelected?: string | null;
  selectedNames: Array<{ value: string; isExact: boolean }>;
  selectedContainers: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onNameSelect: (e: any, selection: SelectOptionObject) => void;
  onNameClear: () => void;
  onNameFilter: (e: any) => any[];
  onContainerSelect: (e: any, selection: SelectOptionObject) => void;
  onContainerClear: () => void;
  onContainerFilter: (e: any) => any[];
  onRoleToggle: () => void;
  onRoleSelect: (e: any, selection: SelectOptionObject) => void;
  onSearch: () => void;
  onDelete: (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => void;
}
const AddressLinksToggleGroup: React.FunctionComponent<IAddressLinksToggleGroupProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  containerSelected,
  containerInput,
  nameOptions,
  containerOptions,
  roleIsExpanded,
  roleSelected,
  selectedNames,
  selectedContainers,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onNameFilter,
  onContainerSelect,
  onContainerClear,
  onContainerFilter,
  onRoleToggle,
  onRoleSelect,
  onSearch,
  onDelete
}) => {
  const filterMenuItems = [
    { key: "filterName", value: "Name" },
    { key: "filterContainer", value: "Container" },
    { key: "filterRole", value: "Role" }
  ];
  const roleOptions: ISelectOption[] = [
    { value: "Sender", isDisabled: false },
    { value: "Receiver", isDisabled: false }
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
      <DataToolbarItem
        breakpointMods={[{ modifier: "spacer-none", breakpoint: "md" }]}
      >
        <DataToolbarFilter
          chips={selectedNames.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Name"
        >
          {filterSelected && filterSelected === "Name" && (
            <InputGroup>
              <TypeAhead
                ariaLabelTypeAhead={"Select name"}
                ariaLabelledBy={"typeahead-select-id"}
                onSelect={onNameSelect}
                onClear={onNameClear}
                onFilter={onNameFilter}
                selected={nameSelected}
                inputData={nameInput || ""}
                options={nameOptions}
                placeholderText={"Select name"}
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
          chips={selectedContainers.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Container"
        >
          {filterSelected && filterSelected === "Container" && (
            <InputGroup>
              <TypeAhead
                ariaLabelTypeAhead={"Select container"}
                ariaLabelledBy={"typeahead-select-id"}
                onSelect={onContainerSelect}
                onClear={onContainerClear}
                onFilter={onContainerFilter}
                selected={containerSelected}
                inputData={containerInput || ""}
                options={containerOptions}
                placeholderText={"Select container"}
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
        </DataToolbarFilter>
      </DataToolbarItem>
      <DataToolbarItem
        breakpointMods={[{ modifier: "spacer-none", breakpoint: "md" }]}
      >
        <DataToolbarFilter
          chips={roleSelected ? [roleSelected] : []}
          deleteChip={onDelete}
          categoryName="Role"
        >
          {filterSelected === "Role" && (
            <Select
              variant={SelectVariant.single}
              aria-label="Select Role"
              onToggle={onRoleToggle}
              onSelect={onRoleSelect}
              selections={roleSelected || "Select Role"}
              isExpanded={roleIsExpanded}
            >
              {roleOptions.map((option, index) => (
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
export { AddressLinksToggleGroup };
