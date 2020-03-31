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
import { TypeAhead, DropdownWithToggle } from "components";

export interface IConnectionsToggleGroupProps {
  totalRecords: number;
  filterSelected?: string;
  hostnameSelected?: string;
  hostnameInput?: string;
  containerSelected?: string;
  containerInput?: string;
  hostnameOptions?: any[];
  containerOptions?: any[];
  selectedHostnames: Array<{ value: string; isExact: boolean }>;
  selectedContainers: Array<{ value: string; isExact: boolean }>;
  onFilterSelect: (value: string) => void;
  onHostnameSelect: (e: any, selection: SelectOptionObject) => void;
  onHostnameClear: () => void;
  onHostnameFilter: (e: any) => any[];
  onContainerSelect: (e: any, selection: SelectOptionObject) => void;
  onContainerClear: () => void;
  onContainerFilter: (e: any) => any[];
  onSearch: () => void;
  onDelete: (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => void;
}
const ConnectionsToggleGroup: React.FunctionComponent<IConnectionsToggleGroupProps> = ({
  totalRecords,
  filterSelected,
  hostnameSelected,
  hostnameInput,
  containerSelected,
  containerInput,
  hostnameOptions,
  containerOptions,
  selectedHostnames,
  selectedContainers,
  onFilterSelect,
  onHostnameSelect,
  onHostnameClear,
  onHostnameFilter,
  onContainerSelect,
  onContainerClear,
  onContainerFilter,
  onSearch,
  onDelete
}) => {
  const filterMenuItems = [
    { key: "filterHostname", value: "Hostname" },
    { key: "filterContainer", value: "Container" }
  ];
  const roleOptions: ISelectOption[] = [
    { value: "Sender", isDisabled: false },
    { value: "Receiver", isDisabled: false }
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
      <DataToolbarItem
        breakpointMods={[{ modifier: "spacer-none", breakpoint: "md" }]}
      >
        <DataToolbarFilter
          chips={selectedHostnames.map(filter => filter.value)}
          deleteChip={onDelete}
          categoryName="Hostname"
        >
          {filterSelected && filterSelected === "Hostname" && (
            <InputGroup>
              <TypeAhead
                ariaLabelTypeAhead={"Select hostname"}
                ariaLabelledBy={"typeahead-select-id"}
                onSelect={onHostnameSelect}
                onClear={onHostnameClear}
                onFilter={onHostnameFilter}
                selected={hostnameSelected}
                inputData={hostnameInput || ""}
                options={hostnameOptions}
                placeholderText={"Select hostname"}
              />
              <Button
                id="ad-links-filter-search-hostname"
                variant={ButtonVariant.control}
                aria-label="search button for search hostname"
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
export { ConnectionsToggleGroup };
