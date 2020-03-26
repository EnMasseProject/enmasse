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
import { DataToolbarBreakpointMod } from "@patternfly/react-core/dist/js/components/DataToolbar/DataToolbarUtils";

interface IMessagingToolbarToggleGroupProps {
  totalRecords: number;
  filterSelected?: string;
  nameSelected?: string;
  nameInput?: string;
  namespaceSelected?: string;
  namespaceInput?: string;
  nameOptions?: any[];
  namespaceOptions?: any[];
  typeIsExpanded: boolean;
  typeSelected?: string | null;
  selectedNames: Array<{ value: string; isExact: boolean }>;
  selectedNamespaces: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onNameSelect: (e: any, selection: SelectOptionObject) => void;
  onNameClear: () => void;
  onNameFilter: (e: any) => any[];
  onNamespaceSelect: (e: any, selection: SelectOptionObject) => void;
  onNamespaceClear: () => void;
  onNamespaceFilter: (e: any) => any[];
  onTypeToggle: () => void;
  onTypeSelect: (e: any, selection: SelectOptionObject) => void;
  onDeleteAll: () => void;
  onSearch: () => void;
  onDelete: (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => void;
}
const MessagingToolbarToggleGroup: React.FunctionComponent<IMessagingToolbarToggleGroupProps> = ({
  totalRecords,
  filterSelected,
  nameSelected,
  nameInput,
  namespaceSelected,
  namespaceInput,
  nameOptions,
  namespaceOptions,
  typeIsExpanded,
  typeSelected,
  selectedNames,
  selectedNamespaces,
  onFilterSelect,
  onNameSelect,
  onNameClear,
  onNameFilter,
  onNamespaceSelect,
  onNamespaceClear,
  onNamespaceFilter,
  onTypeToggle,
  onTypeSelect,
  onDeleteAll,
  onSearch,
  onDelete
}) => {
  const filterMenuItems = [
    { key: "filterName", value: "Name" },
    { key: "filterNamespace", value: "Namespace" },
    { key: "filterType", value: "Type" }
  ];
  const typeOptions: ISelectOption[] = [
    { value: "Standard", isDisabled: false },
    { value: "Brokered", isDisabled: false }
  ];

  const checkIsFilterApplied = () => {
    if (
      (selectedNames && selectedNames.length > 0) ||
      (selectedNamespaces && selectedNamespaces.length > 0) ||
      (typeSelected && typeSelected.trim() !== "")
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
          chips={selectedNamespaces.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Namespace"
        >
          {filterSelected && filterSelected === "Namespace" && (
            <InputGroup>
              <TypeAhead
                ariaLabelTypeAhead={"Select namespace"}
                ariaLabelledBy={"typeahead-select-id"}
                onSelect={onNamespaceSelect}
                onClear={onNamespaceClear}
                onFilter={onNamespaceFilter}
                selected={namespaceSelected}
                inputData={namespaceInput || ""}
                options={namespaceOptions}
                placeholderText={"Select namespace"}
              />
              <Button
                id="ad-links-filter-search-namespace"
                variant={ButtonVariant.control}
                aria-label="search button for search namespace"
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
          {filterSelected === "Type" && (
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
export { MessagingToolbarToggleGroup };
