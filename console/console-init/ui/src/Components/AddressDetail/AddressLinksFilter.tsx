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
  Badge,
  Split,
  SplitItem
} from "@patternfly/react-core";
import {
  FilterIcon,
  SearchIcon,
  SortAlphaDownAltIcon,
  SortAmountDownIcon,
  SortAmountUpAltIcon,
  SortAmountDownAltIcon
} from "@patternfly/react-icons";
import { ISortBy } from "@patternfly/react-table";
import useWindowDimensions from "../Common/WindowDimension";
import style from "react-syntax-highlighter/dist/styles/hljs/atelier-dune-light";

interface IAddressLinksFilterProps {
  filterValue: string;
  setFilterValue: (value: string) => void;
  filterNames: string[];
  setFilterNames: (value: Array<string>) => void;
  filterContainers: string[];
  setFilterContainers: (value: Array<string>) => void;
  filterRole?: string;
  setFilterRole: (role: string | undefined) => void;
  totalLinks: number;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  namesOptions: Array<{ value: string }>;
  onNameChange: (newValue: string) => void;
}

export const AddressLinksFilter: React.FunctionComponent<IAddressLinksFilterProps> = ({
  filterValue,
  setFilterValue,
  filterNames,
  setFilterNames,
  filterContainers,
  setFilterContainers,
  filterRole,
  setFilterRole,
  totalLinks,
  sortValue,
  setSortValue,
  namesOptions,
  onNameChange
}) => {
  const [inputValue, setInputValue] = React.useState<string>();
  const [filterIsExpanded, setFilterIsExpanded] = React.useState(false);
  const [roleIsExpanded, setRoleIsExpanded] = React.useState(false);
  const [sortIsExpanded, setSortIsExpanded] = React.useState<boolean>(false);
  const { width } = useWindowDimensions();
  const [sortData, setSortData] = React.useState();
  const [sortDirection, setSortDirection] = React.useState<string>();
  const filterMenuItems = [
    { key: "filterName", value: "Name" },
    { key: "filterContainers", value: "Container" },
    { key: "filterRole", value: "Role" }
  ];

  const roleMenuItems = [
    { key: "roleSender", value: "Sender" },
    { key: "roleReceiver", value: "Receiver" }
  ];

  const sortMenuItems = [
    { key: "name", value: "Name", index: 2 },
    { key: "deliveryRate", value: "DeliveryRate", index: 3 },
    { key: "backlog", value: "Backlog", index: 4 }
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
      setInputValue(undefined);
    } else if (filterValue && filterValue === "Container") {
      if (inputValue && inputValue.trim() !== "")
        if (filterContainers.indexOf(inputValue.trim()) < 0) {
          setFilterContainers([...filterContainers, inputValue.trim()]);
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
      case "Container":
        if (filterContainers && id) {
          let index = filterContainers.indexOf(id.toString());
          if (index >= 0) filterContainers.splice(index, 1);
          setFilterContainers([...filterContainers]);
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
      (filterContainers && filterContainers.length > 0) ||
      (filterRole && filterRole.trim() !== "")
    ) {
      return true;
    }
    return false;
  };
  const onDeleteAll = () => {
    setFilterValue("Name");
    setFilterNames([]);
    setFilterContainers([]);
    setFilterRole(undefined);
  };

  const onSortSelect = (event: any) => {
    setSortData(event.target.value);
    setSortDirection(undefined);
    setSortIsExpanded(!sortIsExpanded);
  };

  const onSortUp = () => {
    if (sortData) {
      const sortItem = sortMenuItems.filter(
        object => object.value === sortData
      );
      setSortValue({ index: sortItem[0].index, direction: "asc" });
      setSortDirection("asc");
    }
  };
  const onSortDown = () => {
    if (sortData) {
      const sortItem = sortMenuItems.filter(
        object => object.value === sortData
      );
      setSortValue({ index: sortItem[0].index, direction: "desc" });
      setSortDirection("desc");
    }
  };

  const SortIcons = (
    <>
      {!sortDirection ? (
        <SortAmountDownAltIcon color="grey" onClick={onSortUp} />
      ) : sortDirection === "asc" ? (
        <SortAmountUpAltIcon color="blue" onClick={onSortDown} />
      ) : (
        <SortAmountDownAltIcon color="blue" onClick={onSortUp} />
      )}
    </>
  );

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
              chips={filterContainers}
              deleteChip={onDelete}
              categoryName="Container">
              {filterValue && filterValue === "Container" && (
                <InputGroup>
                  <TextInput
                    name="container"
                    id="container"
                    type="search"
                    aria-label="search input container"
                    placeholder="Filter By Container ..."
                    onChange={onInputChange}
                    value={inputValue || ""}
                  />
                  <Button
                    variant={ButtonVariant.control}
                    aria-label="search button for search containers"
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
          <DataToolbarItem>
            {width < 769 && (
              <>
                Sort &nbsp;
                <Dropdown
                  position="left"
                  onSelect={onSortSelect}
                  isOpen={sortIsExpanded}
                  toggle={
                    <DropdownToggle onToggle={setSortIsExpanded}>
                      {sortData}
                    </DropdownToggle>
                  }
                  dropdownItems={sortMenuItems.map(option => (
                    <DropdownItem
                      key={option.key}
                      value={option.value}
                      itemID={option.key}
                      component={"button"}>
                      {option.value}
                    </DropdownItem>
                  ))}
                />
                {SortIcons}
              </>
            )}
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
      </DataToolbarContent>
    </DataToolbar>
  );
};
