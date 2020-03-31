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

export interface IConnectionLinksToggleGroupProps {
  totalRecords: number;
  filterSelected?: string;
  nameSelected?: string;
  nameInput?: string;
  addressSelected?: string;
  addressInput?: string;
  nameOptions?: any[];
  addressOptions?: any[];
  roleIsExpanded: boolean;
  roleSelected?: string | null;
  selectedNames: Array<{ value: string; isExact: boolean }>;
  selectedAddresses: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onNameSelect: (e: any, selection: SelectOptionObject) => void;
  onNameClear: () => void;
  onNameFilter: (e: any) => any[];
  onAddressSelect: (e: any, selection: SelectOptionObject) => void;
  onAddressClear: () => void;
  onAddressFilter: (e: any) => any[];
  onRoleToggle: () => void;
  onRoleSelect: (e: any, selection: SelectOptionObject) => void;
  onSearch: () => void;
  onDelete: (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => void;
}
const ConnectionLinksToggleGroup: React.FunctionComponent<IConnectionLinksToggleGroupProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  addressSelected,
  addressInput,
  nameOptions,
  addressOptions,
  roleIsExpanded,
  roleSelected,
  selectedNames,
  selectedAddresses,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onNameFilter,
  onAddressSelect,
  onAddressClear,
  onAddressFilter,
  onRoleToggle,
  onRoleSelect,
  onSearch,
  onDelete
}) => {
  const filterMenuItems = [
    { key: "filterName", value: "Name" },
    { key: "filterAddress", value: "Address" },
    { key: "filterRole", value: "Role" }
  ];
  const roleOptions: ISelectOption[] = [
    { value: "Sender", isDisabled: false },
    { value: "Receiver", isDisabled: false }
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
          chips={selectedAddresses.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Address"
        >
          {filterSelected && filterSelected === "Address" && (
            <InputGroup>
              <TypeAhead
                ariaLabelTypeAhead={"Select address"}
                ariaLabelledBy={"typeahead-select-id"}
                onSelect={onAddressSelect}
                onClear={onAddressClear}
                onFilter={onAddressFilter}
                selected={addressSelected}
                inputData={addressInput || ""}
                options={addressOptions}
                placeholderText={"Select address"}
              />
              <Button
                id="ad-links-filter-search-address"
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
export { ConnectionLinksToggleGroup };
