/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
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
import useWindowDimensions from "components/common/WindowDimension";
import { SortForMobileView } from "components/common/SortForMobileView";
import { useApolloClient } from "@apollo/react-hooks";
import {
  IConnectionLinksNameSearchResponse,
  IConnectionLinksAddressSearchResponse
} from "types/ResponseTypes";
import {
  RETURN_ALL_CONNECTION_LINKS_FOR_NAME_SEARCH,
  RETURN_ALL_CONNECTION_LINKS_FOR_ADDRESS_SEARCH
} from "graphql-module/queries";
import {
  TypeAheadMessage,
  TYPEAHEAD_REQUIRED_LENGTH,
  FetchPolicy
} from "constants/constants";
import { getSelectOptionList, ISelectOption } from "utils";

interface IConnectionDetailFilterProps {
  filterValue: string;
  setFilterValue: (value: string) => void;
  filterNames: any[];
  setFilterNames: (value: Array<string>) => void;
  filterAddresses: any[];
  setFilterAddresses: (value: Array<string>) => void;
  filterRole?: string;
  setFilterRole: (role: string | undefined) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  totalLinks: number;
  namespace: string;
  connectionName: string;
}

export const ConnectionDetailFilter: React.FunctionComponent<IConnectionDetailFilterProps> = ({
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
  const [nameInput, setNameInput] = React.useState<string>("");
  const [addressInput, setAddressInput] = React.useState<string>("");
  const [nameOptions, setNameOptions] = React.useState<Array<ISelectOption>>();
  const [addressOptions, setAddressOptions] = React.useState<
    Array<ISelectOption>
  >();

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
    { key: "accepted", value: "Accepted", index: 4 },
    { key: "rejected", value: "Rejected", index: 5 },
    { key: "released", value: "Released", index: 6 },
    { key: "modified", value: "Modified", index: 7 },
    { key: "presettled", value: "Presettled", index: 8 },
    { key: "undelievered", value: "Undelievered", index: 9 }
  ];

  const onSearch = (event: any) => {
    if (filterValue && filterValue === "Name") {
      if (nameSelected && nameSelected.trim() !== "")
        if (
          filterNames.map(filter => filter.value).indexOf(nameSelected.trim()) <
          0
        ) {
          setFilterNames([
            ...filterNames,
            { value: nameSelected.trim(), isExact: true }
          ]);
          setNameSelected(undefined);
        }
      if (!nameSelected && nameInput && nameInput.trim() !== "")
        if (
          filterNames.map(filter => filter.value).indexOf(nameInput.trim()) < 0
        )
          setFilterNames([
            ...filterNames,
            { value: nameInput.trim(), isExact: false }
          ]);
    } else if (filterValue && filterValue === "Address") {
      if (addressSelected && addressSelected.trim() !== "")
        if (
          filterAddresses
            .map(filter => filter.value)
            .indexOf(addressSelected.trim()) < 0
        ) {
          setFilterAddresses([
            ...filterAddresses,
            { value: addressSelected.trim(), isExact: true }
          ]);
          setAddressSelected(undefined);
        }
      if (!addressSelected && addressInput && addressInput.trim() !== "")
        if (
          filterAddresses
            .map(filter => filter.value)
            .indexOf(addressInput.trim()) < 0
        )
          setFilterAddresses([
            ...filterAddresses,
            { value: addressInput.trim(), isExact: false }
          ]);
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
    if (value.trim().length < TYPEAHEAD_REQUIRED_LENGTH) {
      setNameOptions([]);
      return;
    }
    const response = await client.query<IConnectionLinksNameSearchResponse>({
      query: RETURN_ALL_CONNECTION_LINKS_FOR_NAME_SEARCH(
        connectionName,
        namespace,
        value.trim()
      ),
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    if (
      response &&
      response.data &&
      response.data.connections &&
      response.data.connections.connections &&
      response.data.connections.connections.length > 0 &&
      response.data.connections.connections[0].links &&
      response.data.connections.connections[0].links.links &&
      response.data.connections.connections[0].links.links.length > 0
    ) {
      const obtainedList = response.data.connections.connections[0].links.links.map(
        (link: any) => {
          return link.metadata.name;
        }
      );
      //get list of unique records to display in the select dropdown based on total records and 100 fetched objects
      const uniqueList = getSelectOptionList(
        obtainedList,
        response.data.connections.connections[0].links.total
      );
      if (uniqueList.length > 0) setNameOptions(uniqueList);
    }
  };

  const onNameSelectFilterChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setNameInput(e.target.value);
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
    if (value.trim().length < TYPEAHEAD_REQUIRED_LENGTH) {
      setAddressOptions([]);
      return;
    }
    const response = await client.query<IConnectionLinksAddressSearchResponse>({
      query: RETURN_ALL_CONNECTION_LINKS_FOR_ADDRESS_SEARCH(
        connectionName,
        namespace,
        value.trim()
      ),
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    if (
      response &&
      response.data &&
      response.data.connections &&
      response.data.connections.connections &&
      response.data.connections.connections.length > 0 &&
      response.data.connections.connections[0].links &&
      response.data.connections.connections[0].links.links &&
      response.data.connections.connections[0].links.links.length > 0
    ) {
      const obtainedList = response.data.connections.connections[0].links.links.map(
        (link: any) => {
          return link.spec.address;
        }
      );
      //get list of unique records to display in the select dropdown based on total records and 100 fetched objects
      const uniqueList = getSelectOptionList(
        obtainedList,
        response.data.connections.connections[0].links.total
      );
      if (uniqueList.length > 0) setAddressOptions(uniqueList);
    }
  };
  const onAddressSelectFilterChange = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setAddressInput(e.target.value);
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
          let index = filterNames
            .map(filter => filter.value)
            .indexOf(id.toString());
          if (index >= 0) filterNames.splice(index, 1);
          setFilterNames([...filterNames]);
        }
        break;
      case "Address":
        if (filterAddresses && id) {
          let index = filterAddresses
            .map(filter => filter.value)
            .indexOf(id.toString());
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
              chips={filterNames.map(filter => filter.value)}
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
                    maxHeight="200px"
                    selections={nameSelected}
                    onFilter={onNameSelectFilterChange}
                    isExpanded={isSelectNameExpanded}
                    ariaLabelledBy={"typeahead-select-id"}
                    placeholderText="Select name"
                    isDisabled={false}
                    isCreatable={false}
                  >
                    {nameOptions && nameOptions.length > 0 ? (
                      nameOptions.map((option, index) => (
                        <SelectOption
                          key={index}
                          value={option.value}
                          isDisabled={option.isDisabled}
                        />
                      ))
                    ) : nameInput.trim().length < TYPEAHEAD_REQUIRED_LENGTH ? (
                      <SelectOption
                        key={"invalid-input-length"}
                        value={TypeAheadMessage.MORE_CHAR_REQUIRED}
                        disabled={true}
                      />
                    ) : (
                      <SelectOption
                        key={"no-results-found"}
                        value={TypeAheadMessage.NO_RESULT_FOUND}
                        disabled={true}
                      />
                    )}
                    {/* {} */}
                  </Select>
                  <Button
                    id="cl-filter-search-name"
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
          <DataToolbarItem>
            <DataToolbarFilter
              chips={filterAddresses.map(filter => filter.value)}
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
                    maxHeight="200px"
                    selections={addressSelected}
                    onFilter={onAddressSelectFilterChange}
                    isExpanded={isSelectAddressExpanded}
                    ariaLabelledBy={"typeahead-select-id"}
                    placeholderText="Select Address"
                    isDisabled={false}
                    isCreatable={false}
                  >
                    {addressOptions && addressOptions.length > 0 ? (
                      addressOptions.map((option, index) => (
                        <SelectOption
                          key={index}
                          value={option.value}
                          isDisabled={option.isDisabled}
                        />
                      ))
                    ) : addressInput.trim().length <
                      TYPEAHEAD_REQUIRED_LENGTH ? (
                      <SelectOption
                        key={"invalid-input-length"}
                        value={TypeAheadMessage.MORE_CHAR_REQUIRED}
                        disabled={true}
                      />
                    ) : (
                      <SelectOption
                        key={"no-results-found"}
                        value={TypeAheadMessage.NO_RESULT_FOUND}
                        disabled={true}
                      />
                    )}
                    {/* {} */}
                  </Select>
                  <Button
                    id="cl-filter-search-address"
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
                      {filterRole || "Select Role"}
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
