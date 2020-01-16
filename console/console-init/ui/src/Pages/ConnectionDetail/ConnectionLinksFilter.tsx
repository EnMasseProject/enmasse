/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

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
  Button,
  ButtonVariant,
  Badge,
  SelectOption,
  SelectOptionObject,
  Select,
  SelectVariant
} from "@patternfly/react-core";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import { ISortBy } from "@patternfly/react-table";
import useWindowDimensions from "../../Components/Common/WindowDimension";
import { SortForMobileView } from "../../Components/Common/SortForMobileView";
import { useApolloClient } from "@apollo/react-hooks";
import {
  IConnectionLinksNameSearchResponse,
  IConnectionLinksAddressSearchResponse
} from "src/Types/ResponseTypes";
import {
  RETURN_ALL_CONNECTION_LINKS_FOR_NAME_SEARCH,
  RETURN_ALL_CONNECTION_LINKS_FOR_ADDRESS_SEARCH
} from "src/Queries/Queries";

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
  addressSpaceName: string;
  namespace: string;
  connectionName: string;
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
  totalLinks,
  addressSpaceName,
  namespace,
  connectionName
}) => {
  const { width } = useWindowDimensions();
  const client = useApolloClient();
  const [filterIsExpanded, setFilterIsExpanded] = React.useState(false);
  const [roleIsExpanded, setRoleIsExpanded] = React.useState(false);
  const [isSelectNameExpanded, setIsSelectNameExpanded] = React.useState<
    boolean
  >(false);
  const [isSelectAddressExpanded, setIsSelectAddressExpanded] = React.useState<
    boolean
  >(false);
  const [nameSelected, setNameSelected] = React.useState<string>();
  const [addressSelected, setAddressSelected] = React.useState<string>();
  const [nameOptions, setNameOptions] = React.useState<Array<string>>();
  const [addressOptions, setAddressOptions] = React.useState<Array<string>>();

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

  const onClickSearchIcon = (event: any) => {
    if (filterValue && filterValue === "Name") {
      if (nameSelected && nameSelected.trim() !== "")
        if (filterNames.indexOf(nameSelected.trim()) < 0) {
          setFilterNames([...filterNames, nameSelected.trim()]);
          setNameSelected(undefined);
        }
    } else if (filterValue && filterValue === "Address") {
      if (addressSelected && addressSelected.trim() !== "")
        if (filterAddresses.indexOf(addressSelected.trim()) < 0) {
          setFilterAddresses([...filterAddresses, addressSelected.trim()]);
          setAddressSelected(undefined);
        }
    }
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

  const onAddressSelectToggle = () => {
    setIsSelectAddressExpanded(!isSelectAddressExpanded);
  };

  const onChangeNameData = async (value: string) => {
    setNameOptions(undefined);
    if (value.trim().length < 6) {
      setNameOptions([]);
      return;
    }
    const response = await client.query<IConnectionLinksNameSearchResponse>({
      query: RETURN_ALL_CONNECTION_LINKS_FOR_NAME_SEARCH(
        connectionName,
        namespace,
        value.trim()
      )
    });
    if (
      response &&
      response.data &&
      response.data.connections &&
      response.data.connections.Connections &&
      response.data.connections.Connections.length > 0 &&
      response.data.connections.Connections[0].Links &&
      response.data.connections.Connections[0].Links.Links &&
      response.data.connections.Connections[0].Links.Links.length > 0
    ) {
      if (response.data.connections.Connections[0].Links.Total > 100) {
        setNameOptions([]);
      } else {
        const obtainedList = response.data.connections.Connections[0].Links.Links.map(
          (link: any) => {
            return link.ObjectMeta.Name;
          }
        );
        setNameOptions(obtainedList);
      }
    }
  };

  const onNameSelectFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChangeNameData(e.target.value);
    const options: React.ReactElement[] = nameOptions
      ? nameOptions.map((option, index) => (
          <SelectOption key={index} value={option} />
        ))
      : [];
    return options;
  };

  const onChangeAddressData = async (value: string) => {
    setAddressOptions(undefined);
    if (value.trim().length < 6) {
      setAddressOptions([]);
      return;
    }
    const response = await client.query<IConnectionLinksAddressSearchResponse>({
      query: RETURN_ALL_CONNECTION_LINKS_FOR_ADDRESS_SEARCH(
        connectionName,
        namespace,
        value.trim()
      )
    });
    if (
      response &&
      response.data &&
      response.data.connections &&
      response.data.connections.Connections &&
      response.data.connections.Connections.length > 0 &&
      response.data.connections.Connections[0].Links &&
      response.data.connections.Connections[0].Links.Links &&
      response.data.connections.Connections[0].Links.Links.length > 0
    ) {
      if (response.data.connections.Connections[0].Links.Total > 100) {
        setAddressOptions([]);
      } else {
        const obtainedList = response.data.connections.Connections[0].Links.Links.map(
          (link: any) => {
            return link.Spec.Address;
          }
        );
        setAddressOptions(obtainedList);
      }
    }
  };
  const onAddressSelectFilterChange = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    onChangeAddressData(e.target.value);
    const options: React.ReactElement[] = addressOptions
      ? addressOptions.map((option, index) => (
          <SelectOption key={index} value={option} />
        ))
      : [];
    return options;
  };

  const onNameSelect = (event: any, selection: string | SelectOptionObject) => {
    setNameSelected(selection.toString());
    setIsSelectNameExpanded(false);
  };

  const onAddressSelect = (
    event: any,
    selection: string | SelectOptionObject
  ) => {
    setAddressSelected(selection.toString());
    setIsSelectAddressExpanded(false);
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
            id="cl-filter-dropdown"
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
                id={`cl-filter-dropdown-item${option.key}`}
                key={option.key}
                value={option.value}
                itemID={option.key}
                component={"button"}
              >
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
              categoryName="Name"
            >
              {filterValue && filterValue === "Name" && (
                <InputGroup>
                  <Select
                    id="cl-filter-select-name"
                    variant={SelectVariant.typeahead}
                    aria-label="Select a Name"
                    onToggle={onNameSelectToggle}
                    onSelect={onNameSelect}
                    onClear={() => {
                      setNameSelected(undefined);
                      setIsSelectNameExpanded(false);
                    }}
                    selections={nameSelected}
                    onFilter={onNameSelectFilterChange}
                    isExpanded={isSelectNameExpanded}
                    ariaLabelledBy={"typeahead-select-id"}
                    placeholderText="Select name"
                    isDisabled={false}
                    isCreatable={false}
                  >
                    {nameOptions &&
                      nameOptions.map((option, index) => (
                        <SelectOption key={index} value={option} />
                      ))}
                    {/* {} */}
                  </Select>
                  <Button
                    id="cl-filter-search-name"
                    variant={ButtonVariant.control}
                    aria-label="search button for search name"
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
              chips={filterAddresses}
              deleteChip={onDelete}
              categoryName="Address"
            >
              {filterValue && filterValue === "Address" && (
                <InputGroup>
                  <Select
                    id="cl-filter-select-address"
                    variant={SelectVariant.typeahead}
                    aria-label="Select a Address"
                    onToggle={onAddressSelectToggle}
                    onSelect={onAddressSelect}
                    onClear={() => {
                      setAddressSelected(undefined);
                      setIsSelectAddressExpanded(false);
                    }}
                    selections={addressSelected}
                    onFilter={onAddressSelectFilterChange}
                    isExpanded={isSelectAddressExpanded}
                    ariaLabelledBy={"typeahead-select-id"}
                    placeholderText="Select Address"
                    isDisabled={false}
                    isCreatable={false}
                  >
                    {addressOptions &&
                      addressOptions.map((option, index) => (
                        <SelectOption key={index} value={option} />
                      ))}
                    {/* {} */}
                  </Select>
                  <Button
                    id="cl-filter-search-address"
                    variant={ButtonVariant.control}
                    aria-label="search button for search address"
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
              chips={filterRole ? [filterRole] : []}
              deleteChip={onDelete}
              categoryName="Role"
            >
              {filterValue === "Role" && (
                <Dropdown
                  id="cl-filter-dropdown-role"
                  position="left"
                  onSelect={onRoleSelect}
                  isOpen={roleIsExpanded}
                  toggle={
                    <DropdownToggle onToggle={setRoleIsExpanded}>
                      <FilterIcon />
                      &nbsp;{filterRole || "Select Role"}
                    </DropdownToggle>
                  }
                  dropdownItems={roleMenuItems.map(option => (
                    <DropdownItem
                      id={`cl-filter-dropdown-role${option.key}`}
                      key={option.key}
                      value={option.value}
                      itemID={option.key}
                      component={"button"}
                    >
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
      clearAllFilters={onDeleteAll}
    >
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
          breakpoint="xl"
        >
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
