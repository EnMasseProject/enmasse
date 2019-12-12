import * as React from "react";
import {
  DataToolbarGroup,
  DataToolbarFilter,
  DataToolbarItem,
  DataToolbar,
  DataToolbarContent,
  DataToolbarChip,
  DataToolbarToggleGroup
} from "@patternfly/react-core/dist/js/experimental";
import {
  DropdownToggle,
  Dropdown,
  DropdownItem,
  InputGroup,
  TextInput,
  Button,
  ButtonVariant,
  Badge
} from "@patternfly/react-core";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";

export interface IOption {
  value: string;
  id: string;
}

export interface IConnectionListFilterProps {
  filterValue?: string | null;
  setFilterValue: (value: string) => void;
  filterIsExpanded: boolean;
  setFilterIsExpanded: (value: boolean) => void;
  hosts?: Array<string>;
  setHosts: (value: Array<string>) => void;
  containerIds?: Array<string>;
  setContainerIds: (value: Array<string>) => void;
  totalConnections: number;
}

export const ConnectionListFilterPage: React.FunctionComponent<IConnectionListFilterProps> = ({
  filterValue,
  setFilterValue,
  filterIsExpanded,
  setFilterIsExpanded,
  hosts,
  setHosts,
  containerIds,
  setContainerIds,
  totalConnections
}) => {
  const [inputValue, setInputValue] = React.useState<string | null>();
  const onFilterSelect = (event: any) => {
    setFilterValue(event.target.value);
    setFilterIsExpanded(!filterIsExpanded);
  };

  const filterMenuItems = [
    { key: "filterHostName", value: "Hostname" },
    { key: "filterContainer", value: "Container" }
  ];

  const onInputChange = (newValue: string) => {
    setInputValue(newValue);
  };
  const onAddInput = (event: any) => {
    if (filterValue)
      if (filterValue === "Container") {
        if (inputValue && inputValue.trim() != "" && containerIds) {
          if (containerIds.indexOf(inputValue) < 0) {
            setContainerIds([...containerIds, inputValue]);
          }
          setInputValue(null);
        }
      } else if (filterValue === "Hostname") {
        if (inputValue && inputValue.trim() != "" && hosts) {
          if (hosts.indexOf(inputValue) < 0) {
            setHosts([...hosts, inputValue]);
          }
          setInputValue(null);
        }
      }
  };

  const onDelete = (
    type: string | DataToolbarChip,
    id: string | DataToolbarChip
  ) => {
    let index;

    switch (type) {
      case "Hostname":
        if (hosts && id) {
          index = hosts.indexOf(id.toString());
          if (index >= 0) hosts.splice(index, 1);
          setHosts([...hosts]);
        }
        break;
      case "Container":
        if (containerIds && id) {
          const containers = containerIds;
          index = containerIds.indexOf(id.toString());
          if (index >= 0) containers.splice(index, 1);
          setContainerIds([...containers]);
        }
        break;
    }
  };
  const clearAllFilters = () => {
    setHosts([]);
    setContainerIds([]);
  };

  const checkIsFilterApplied = () => {
    if (
      (containerIds && containerIds.length > 0) ||
      (hosts && hosts.length > 0)
    ) {
      return true;
    }
    return false;
  };

  const toggleGroupItems = (
    <>
      <DataToolbarGroup variant="filter-group">
        <DataToolbarFilter categoryName="Filter">
          <Dropdown
            id="cl-filter-dropdown"
            position="left"
            onSelect={onFilterSelect}
            isOpen={filterIsExpanded}
            toggle={
              <DropdownToggle onToggle={setFilterIsExpanded}>
                <FilterIcon />
                &nbsp;
                {filterValue && filterValue.trim() !== ""
                  ? filterValue
                  : "Filter"}
              </DropdownToggle>
            }
            dropdownItems={filterMenuItems.map(option => (
              <DropdownItem
                key={option.key}
                value={option.value}
                itemID={option.key}
                component={"button"}>
                {option.value}
              </DropdownItem>
            ))}
          />
        </DataToolbarFilter>
        <DataToolbarItem>
          <DataToolbarFilter
            chips={hosts}
            deleteChip={onDelete}
            categoryName="Hostname">
            {filterValue && filterValue === "Hostname" && (
              <InputGroup>
                <TextInput
                  name="hostname"
                  id="hostname"
                  type="search"
                  aria-label="search input example"
                  placeholder="Filter By Hostname ..."
                  onChange={onInputChange}
                  value={inputValue || ""}
                />
                <Button
                  id="cl-filter-search-btn"
                  variant={ButtonVariant.control}
                  aria-label="search button for search input"
                  onClick={onAddInput}>
                  <SearchIcon />
                </Button>
              </InputGroup>
            )}
          </DataToolbarFilter>
        </DataToolbarItem>
        <DataToolbarItem>
          <DataToolbarFilter
            chips={containerIds}
            deleteChip={onDelete}
            categoryName="Container">
            {filterValue && filterValue === "Container" && (
              <InputGroup>
                <TextInput
                  name="container"
                  id="container"
                  type="search"
                  aria-label="search container"
                  placeholder="Filter By Container ..."
                  onChange={onInputChange}
                  value={inputValue || ""}
                />
                <Button
                  variant={ButtonVariant.control}
                  aria-label="search button for search input"
                  onClick={onAddInput}>
                  <SearchIcon />
                </Button>
              </InputGroup>
            )}
          </DataToolbarFilter>
        </DataToolbarItem>
      </DataToolbarGroup>
    </>
  );
  const toolbarItems = (
    <DataToolbarToggleGroup
      toggleIcon={
        <>
          <FilterIcon />
          {checkIsFilterApplied() && (
            <Badge key={1} isRead>
              {totalConnections}
            </Badge>
          )}
        </>
      }
      breakpoint="xl">
      {toggleGroupItems}
    </DataToolbarToggleGroup>
  );
  return (
    <>
      <DataToolbar
        id="data-toolbar-with-filter"
        className="pf-m-toggle-group-container"
        collapseListedFiltersBreakpoint="md"
        clearAllFilters={clearAllFilters}>
        <DataToolbarContent>{toolbarItems}</DataToolbarContent>
      </DataToolbar>
    </>
  );
};
