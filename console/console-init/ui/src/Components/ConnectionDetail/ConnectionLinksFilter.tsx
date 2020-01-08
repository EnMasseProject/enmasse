import * as React from "react";
import {
  DataToolbarGroup,
  DataToolbarFilter,
  DataToolbarItem,
  DataToolbarChip,
  DataToolbarToggleGroup,
  DataToolbar,
  DataToolbarContent
} from "@patternfly/react-core/dist/js/experimental";
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  InputGroup,
  TextInput,
  Button,
  ButtonVariant,
  Badge
} from "@patternfly/react-core";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import { ISortBy } from "@patternfly/react-table";
import useWindowDimensions from "../Common/WindowDimension";
import { SortForMobileView } from "../Common/SortForMobileView";

interface IConnectionLinksFilterProps {
  filterValue: string;
  setFilterValue: (value: string) => void;
  filterNames: string[];
  setFilterNames: (value: Array<string>) => void;
  filterAddresses: string[];
  setFilterAddresses: (value: Array<string>) => void;
  filterRole?: string;
  setFilterRole: (role: string | undefined) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  totalLinks: number;
}

export const ConnectionLinksFilter: React.FunctionComponent<IConnectionLinksFilterProps> = ({
  filterValue,
  setFilterValue,
  filterNames,
  setFilterNames,
  filterAddresses,
  setFilterAddresses,
  filterRole,
  setFilterRole,
  sortValue,
  setSortValue,
  totalLinks
}) => {
  const [inputValue, setInputValue] = React.useState<string>();
  const [filterIsExpanded, setFilterIsExpanded] = React.useState(false);
  const [roleIsExpanded, setRoleIsExpanded] = React.useState(false);
  const { width } = useWindowDimensions();

  const filterMenuItems = [
    { key: "filterName", value: "Name" },
    { key: "filterAddress", value: "Address" },
    { key: "filterRole", value: "Role" }
  ];

  const roleMenuItems = [
    { key: "roleSender", value: "Sender" },
    { key: "roleReceiver", value: "Receiver" }
  ];

  const sortMenuItems = [
    { key: "name", value: "Name", index: 1 },
    { key: "address", value: "Address", index: 2 },
    { key: "deliveries", value: "Deliveries", index: 3 },
    { key: "rejected", value: "Rejected", index: 4 },
    { key: "released", value: "Released", index: 5 },
    { key: "modified", value: "Modified", index: 6 },
    { key: "presettled", value: "Presettled", index: 7 },
    { key: "undelievered", value: "Undelievered", index: 8 }
  ];

  const onInputChange = (newValue: string) => {
    setInputValue(newValue);
  };

  const onAddInput = (event: any) => {
    if (filterValue && filterValue === "Name") {
      if (inputValue && inputValue.trim() !== "")
        if (filterNames.indexOf(inputValue.trim()) < 0) {
          setFilterNames([...filterNames, inputValue.trim()]);
        }
    } else if (filterValue && filterValue === "Address") {
      if (inputValue && inputValue.trim() !== "")
        if (filterAddresses.indexOf(inputValue.trim()) < 0) {
          setFilterAddresses([...filterAddresses, inputValue.trim()]);
        }
    }
    setInputValue(undefined);
  };

  const onFilterSelect = (event: any) => {
    setFilterValue(event.target.value);
    setFilterIsExpanded(!filterIsExpanded);
  };
  const onRoleSelect = (event: any) => {
    setFilterRole(event.target.value);
    setRoleIsExpanded(!roleIsExpanded);
  };

  const onDelete = (
    type: string | DataToolbarChip,
    id: string | DataToolbarChip
  ) => {
    switch (type) {
      case "Name":
        if (filterNames && id) {
          let index = filterNames.indexOf(id.toString());
          if (index >= 0) filterNames.splice(index, 1);
          setFilterNames([...filterNames]);
        }
        break;
      case "Address":
        if (filterAddresses && id) {
          let index = filterAddresses.indexOf(id.toString());
          if (index >= 0) filterAddresses.splice(index, 1);
          setFilterAddresses([...filterAddresses]);
        }
        break;
      case "Role":
        setFilterRole(undefined);
        break;
    }
  };
  const checkIsFilterApplied = () => {
    if (
      (filterNames && filterNames.length > 0) ||
      (filterAddresses && filterAddresses.length > 0) ||
      (filterRole && filterRole.trim() !== "")
    ) {
      return true;
    }
    return false;
  };
  const onDeleteAll = () => {
    setFilterValue("Name");
    setFilterNames([]);
    setFilterAddresses([]);
    setFilterRole(undefined);
  };
  const toggleGroupItems = (
    <>
      <DataToolbarGroup variant="filter-group">
        <DataToolbarFilter categoryName="Filter">
          <Dropdown
            position="left"
            onSelect={onFilterSelect}
            isOpen={filterIsExpanded}
            toggle={
              <DropdownToggle onToggle={setFilterIsExpanded}>
                <FilterIcon />
                &nbsp;
                {filterValue}
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
        <>
          <DataToolbarItem>
            <DataToolbarFilter
              chips={filterNames}
              deleteChip={onDelete}
              categoryName="Name">
              {filterValue && filterValue === "Name" && (
                <InputGroup>
                  <TextInput
                    name="name"
                    id="name"
                    type="search"
                    aria-label="search input name"
                    placeholder="Filter By Name ..."
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
          <DataToolbarItem>
            <DataToolbarFilter
              chips={filterAddresses}
              deleteChip={onDelete}
              categoryName="Address">
              {filterValue && filterValue === "Address" && (
                <InputGroup>
                  <TextInput
                    name="address"
                    id="address"
                    type="search"
                    aria-label="search input address"
                    placeholder="Filter By Address ..."
                    onChange={onInputChange}
                    value={inputValue || ""}
                  />
                  <Button
                    variant={ButtonVariant.control}
                    aria-label="search button for search address"
                    onClick={onAddInput}>
                    <SearchIcon />
                  </Button>
                </InputGroup>
              )}
            </DataToolbarFilter>
          </DataToolbarItem>

          <DataToolbarItem>
            <DataToolbarFilter
              chips={filterRole ? [filterRole] : []}
              deleteChip={onDelete}
              categoryName="Role">
              {filterValue === "Role" && (
                <Dropdown
                  position="left"
                  onSelect={onRoleSelect}
                  isOpen={roleIsExpanded}
                  toggle={
                    <DropdownToggle onToggle={setRoleIsExpanded}>
                      <FilterIcon />
                      &nbsp;{filterRole}
                    </DropdownToggle>
                  }
                  dropdownItems={roleMenuItems.map(option => (
                    <DropdownItem
                      key={option.key}
                      value={option.value}
                      itemID={option.key}
                      component={"button"}>
                      {option.value}
                    </DropdownItem>
                  ))}
                />
              )}
            </DataToolbarFilter>
          </DataToolbarItem>
        </>
      </DataToolbarGroup>
    </>
  );

  return (
    <DataToolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onDeleteAll}>
      <DataToolbarContent>
        <DataToolbarToggleGroup
          toggleIcon={
            <>
              <FilterIcon />
              {checkIsFilterApplied() && (
                <Badge key={1} isRead>
                  {totalLinks}
                </Badge>
              )}
            </>
          }
          breakpoint="xl">
          {toggleGroupItems}
        </DataToolbarToggleGroup>
        {width < 769 && (
        <SortForMobileView
          sortMenu={sortMenuItems}
          sortValue={sortValue}
          setSortValue={setSortValue}
        />
      )}
      </DataToolbarContent>
    </DataToolbar>
  );
};
