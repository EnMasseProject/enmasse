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
  Select,
  SelectVariant,
  SelectOption,
  SelectOptionObject
} from "@patternfly/react-core";
import {
  FilterIcon,
  SearchIcon
} from "@patternfly/react-icons";
import { ISortBy } from "@patternfly/react-table";
import useWindowDimensions from "../Common/WindowDimension";
import { SortForMobileView } from "../Common/SortForMobileView";
import { useApolloClient } from "@apollo/react-hooks";
import { RETURN_ALL_NAMES_OF_ADDRESS_LINKS } from "src/Queries/Queries";

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
  addressName: string;
  addressSpaceName: string;
  namespace: string;
}
interface ISearchAddressLinkNameResponse {
  addresses: {
    Total: number;
    Addresses: Array<{
      Links: {
        Links: Array<{
          ObjectMeta: {
            Name: string;
          };
        }>;
      };
    }>;
  };
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
  addressName,
  addressSpaceName,
  namespace
}) => {
  const { width } = useWindowDimensions();
  const client = useApolloClient();
  const [inputValue, setInputValue] = React.useState<string>();
  const [filterIsExpanded, setFilterIsExpanded] = React.useState<boolean>(
    false
  );
  const [roleIsExpanded, setRoleIsExpanded] = React.useState<boolean>(false);
  const [isSelectNameExpanded, setIsSelectNameExpanded] = React.useState<
    boolean
  >(false);
  const [selected, setSelected] = React.useState<string>();
  const [nameOptions, setNameOptions] = React.useState<Array<string>>([]);
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
      if (selected && selected.trim() !== "")
        if (filterNames.indexOf(selected.trim()) < 0) {
          setFilterNames([...filterNames, selected.trim()]);
          setSelected(undefined);
        }
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
  const onNameSelectToggle = () => {
    setIsSelectNameExpanded(!isSelectNameExpanded);
  };
  const changeData = async (value: string) => {
    setNameOptions([]);
    const response = await client.query<ISearchAddressLinkNameResponse>({
      query: RETURN_ALL_NAMES_OF_ADDRESS_LINKS(
        addressName,
        addressSpaceName,
        namespace,
        value.trim()
      )
    });
    if (
      response &&
      response.data &&
      response.data.addresses &&
      response.data.addresses.Addresses &&
      response.data.addresses.Addresses.length > 0 &&
      response.data.addresses.Addresses[0].Links &&
      response.data.addresses.Addresses[0].Links.Links &&
      response.data.addresses.Addresses[0].Links.Links.length > 0
    ) {
      const obtainedList = response.data.addresses.Addresses[0].Links.Links.map(
        (link: any) => {
          return link.ObjectMeta.Name;
        }
      );
      setNameOptions(obtainedList);
    }
  };
  const onNameSelectFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    changeData(e.target.value);
    const options: React.ReactElement[] = nameOptions
      ? nameOptions.map((option, index) => (
          <SelectOption key={index} value={option} />
        ))
      : [];
    return options;
  };

  const onNameSelect = (
    event: any,
    selection: string | SelectOptionObject,
    isPlaceholder?: boolean
  ) => {
    setSelected(selection.toString());
    setIsSelectNameExpanded(false);
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
                  <Select
                    variant={SelectVariant.typeahead}
                    aria-label="Select a Name"
                    onToggle={onNameSelectToggle}
                    onSelect={onNameSelect}
                    onClear={() => {
                      setSelected(undefined);
                      setIsSelectNameExpanded(false);
                    }}
                    selections={selected}
                    onFilter={onNameSelectFilterChange}
                    isExpanded={isSelectNameExpanded}
                    ariaLabelledBy={"typeahead-select-id"}
                    placeholderText="Select name"
                    isDisabled={false}
                    isCreatable={false}
                    onCreateOption={() => {}}>
                    {nameOptions &&
                      nameOptions.map((option, index) => (
                        <SelectOption key={index} value={option} />
                      ))}
                  </Select>
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
        <>
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
        </>
      </DataToolbarContent>
    </DataToolbar>
  );
};
