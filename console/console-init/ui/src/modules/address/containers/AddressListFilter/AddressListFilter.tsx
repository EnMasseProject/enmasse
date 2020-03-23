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
  OverflowMenu,
  OverflowMenuContent,
  OverflowMenuGroup,
  OverflowMenuItem,
  OverflowMenuControl,
  DataToolbarToggleGroup,
  DataToolbarChipGroup
} from "@patternfly/react-core/dist/js/experimental";
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  InputGroup,
  Button,
  ButtonVariant,
  KebabToggle,
  Badge,
  SelectOption,
  Select,
  SelectVariant,
  SelectOptionObject
} from "@patternfly/react-core";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import { useApolloClient } from "@apollo/react-hooks";
import { IAddressListNameSearchResponse } from "types/ResponseTypes";
import { RETURN_ALL_ADDRESS_NAMES_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH } from "graphql-module/queries";
import {
  TypeAheadMessage,
  TYPEAHEAD_REQUIRED_LENGTH,
  FetchPolicy
} from "constants/constants";
import { getSelectOptionList, ISelectOption } from "utils";

interface IAddressListFilterProps {
  filterValue: string | null;
  setFilterValue: (value: string | null) => void;
  filterNames: any[];
  setFilterNames: (value: Array<any>) => void;
  typeValue: string | null;
  setTypeValue: (value: string | null) => void;
  statusValue: string | null;
  setStatusValue: (value: string | null) => void;
  totalAddresses: number;
  addressspaceName?: string;
  namespace?: string;
}

interface IAddressListKebabProps {
  createAddressOnClick: () => void;
  onDeleteAllAddress: () => void;
  onPurgeAllAddress: () => void;
  isDeleteAllDisabled: boolean;
  isPurgeAllDisabled: boolean;
}
export const AddressListFilter: React.FunctionComponent<IAddressListFilterProps> = ({
  filterValue,
  setFilterValue,
  filterNames,
  setFilterNames,
  typeValue,
  setTypeValue,
  statusValue,
  setStatusValue,
  totalAddresses,
  addressspaceName,
  namespace
}) => {
  const client = useApolloClient();
  const [filterIsExpanded, setFilterIsExpanded] = React.useState(false);
  const [typeIsExpanded, setTypeIsExpanded] = React.useState(false);
  const [statusIsExpanded, setStatusIsExpanded] = React.useState(false);
  const [isSelectNameExpanded, setIsSelectNameExpanded] = React.useState<
    boolean
  >(false);
  const [nameSelected, setNameSelected] = React.useState<string>();
  const [nameOptions, setNameOptions] = React.useState<Array<ISelectOption>>();
  const [nameInput, setNameInput] = React.useState<string>("");
  const filterMenuItems = [
    { key: "filterAddress", value: "Address" },
    { key: "filterType", value: "Type" },
    { key: "filterStatus", value: "Status" }
  ];

  const typeMenuItems = [
    { key: "typeQueue", value: "Queue" },
    { key: "typeTopic", value: "Topic" },
    { key: "typeSubscription", value: "Subscription" },
    { key: "typeMulitcast", value: "Mulitcast" },
    { key: "typeAnycast", value: "Anycast" }
  ];

  const statusMenuItems = [
    { key: "statusActive", value: "Active" },
    { key: "statusConfiguring", value: "Configuring" },
    { key: "statusFailed", value: "Failed" }
  ];

  const onClickSearchIcon = (event: any) => {
    if (filterValue && filterValue === "Address") {
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
    }
    if (!nameSelected && nameInput && nameInput.trim() !== "")
      if (filterNames.map(filter => filter.value).indexOf(nameInput.trim()) < 0)
        setFilterNames([
          ...filterNames,
          { value: nameInput.trim(), isExact: false }
        ]);
  };

  const onFilterSelect = (event: any) => {
    setFilterValue(event.target.value);
    setFilterIsExpanded(!filterIsExpanded);
  };
  const onTypeSelect = (event: any) => {
    setTypeValue(event.target.value);
    setTypeIsExpanded(!typeIsExpanded);
  };

  const onStatusSelect = (event: any) => {
    setStatusValue(event.target.value);
    setStatusIsExpanded(!statusIsExpanded);
  };

  const onNameSelectToggle = () => {
    setIsSelectNameExpanded(!isSelectNameExpanded);
  };

  const onChangeNameData = async (value: string) => {
    setNameOptions(undefined);
    if (value.trim().length < TYPEAHEAD_REQUIRED_LENGTH) {
      setNameOptions([]);
      return;
    }
    const response = await client.query<IAddressListNameSearchResponse>({
      query: RETURN_ALL_ADDRESS_NAMES_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH(
        addressspaceName,
        namespace,
        value.trim()
      ),
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    if (
      response &&
      response.data &&
      response.data.addresses &&
      response.data.addresses.addresses &&
      response.data.addresses.addresses.length > 0
    ) {
      const obtainedList = response.data.addresses.addresses.map(
        (address: any) => {
          return address.spec.address;
        }
      );
      //get list of unique records to display in the select dropdown based on total records and 100 fetched objects
      const uniqueList = getSelectOptionList(
        obtainedList,
        response.data.addresses.total
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

  const onNameSelect = (event: any, selection: string | SelectOptionObject) => {
    setNameSelected(selection.toString());
    setIsSelectNameExpanded(false);
  };

  const onDelete = (
    category: string | DataToolbarChipGroup,
    chip: DataToolbarChip | string
  ) => {
    switch (category) {
      case "Address":
        let index;
        if (filterNames && chip) {
          index = filterNames
            .map(filter => filter.value)
            .indexOf(chip.toString());
          if (index >= 0) filterNames.splice(index, 1);
          setFilterNames([...filterNames]);
        }
        break;
      case "Type":
        setTypeValue(null);
        break;
      case "Status":
        setStatusValue(null);
        break;
    }
  };
  const checkIsFilterApplied = () => {
    if (
      (filterNames && filterNames.length > 0) ||
      (typeValue && typeValue.trim() !== "") ||
      (statusValue && statusValue.trim() !== "")
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
            id="al-filter-dropdown"
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
                id={`al-filter-dropdown${option.key}`}
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
        {filterValue && filterValue.trim() !== "" && (
          <>
            <DataToolbarItem>
              <DataToolbarFilter
                chips={filterNames.map(filter => filter.value)}
                deleteChip={onDelete}
                categoryName="Address"
              >
                {filterValue && filterValue === "Address" && (
                  <InputGroup>
                    <Select
                      id="al-filter-select-name"
                      variant={SelectVariant.typeahead}
                      aria-label="Select a Address"
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
                      placeholderText="Select Address"
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
                      ) : nameInput.trim().length <
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
                      id={"al-filter-select-name-search"}
                      variant={ButtonVariant.control}
                      aria-label="search button for search input"
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
                chips={typeValue ? [typeValue] : []}
                deleteChip={onDelete}
                categoryName="Type"
              >
                {filterValue === "Type" && (
                  <Dropdown
                    id={"al-filter-select-type-dropdown"}
                    position="left"
                    onSelect={onTypeSelect}
                    isOpen={typeIsExpanded}
                    toggle={
                      <DropdownToggle onToggle={setTypeIsExpanded}>
                        {typeValue || "Select Type"}
                      </DropdownToggle>
                    }
                    dropdownItems={typeMenuItems.map(option => (
                      <DropdownItem
                        id={`al-filter-select-type-dropdown-item${option.key}`}
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
            <DataToolbarItem>
              <DataToolbarFilter
                chips={statusValue ? [statusValue] : []}
                deleteChip={onDelete}
                categoryName="Status"
              >
                {filterValue === "Status" && (
                  <Dropdown
                    id={"al-filter-select-status-dropdown"}
                    position="left"
                    onSelect={onStatusSelect}
                    isOpen={statusIsExpanded}
                    toggle={
                      <DropdownToggle onToggle={setStatusIsExpanded}>
                        &nbsp;{statusValue || "Select Status"}
                      </DropdownToggle>
                    }
                    dropdownItems={statusMenuItems.map(option => (
                      <DropdownItem
                        id={`al-filter-select-status-dropdown-item${option.key}`}
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
        )}
      </DataToolbarGroup>
    </>
  );

  return (
    <>
      <DataToolbarToggleGroup
        toggleIcon={
          <>
            <FilterIcon />
            {checkIsFilterApplied() && (
              <Badge key={1} isRead>
                {totalAddresses}
              </Badge>
            )}
          </>
        }
        breakpoint="xl"
      >
        {toggleGroupItems}
      </DataToolbarToggleGroup>
    </>
  );
};

export const AddressListKebab: React.FunctionComponent<IAddressListKebabProps> = ({
  createAddressOnClick,
  onDeleteAllAddress,
  onPurgeAllAddress,
  isDeleteAllDisabled,
  isPurgeAllDisabled
}) => {
  const [isKebabOpen, setIsKebabOpen] = React.useState(false);
  const dropdownItems = [
    <DropdownItem
      id="al-filter-dropdown-item-deleteall"
      key="delete-all"
      value="deleteAll"
      component="button"
      isDisabled={isDeleteAllDisabled}
    >
      Delete Selected
    </DropdownItem>,
    <DropdownItem
      id="al-filter-dropdown-item-purgeall"
      key="purge-all"
      value="purgeAll"
      component="button"
      isDisabled={isPurgeAllDisabled}
    >
      Purge Selected
    </DropdownItem>
  ];
  const onKebabToggle = (isOpen: boolean) => {
    setIsKebabOpen(isOpen);
  };

  const onKebabSelect = async (event: any) => {
    if (event.target.value) {
      if (event.target.value === "purgeAll") {
        await onPurgeAllAddress();
      } else if (event.target.value === "deleteAll") {
        await onDeleteAllAddress();
      }
    }
    setIsKebabOpen(!isKebabOpen);
  };
  return (
    <>
      <OverflowMenu breakpoint="lg">
        <OverflowMenuContent isPersistent>
          <OverflowMenuGroup groupType="button" isPersistent>
            {/* Remove is Persistent after fixing dropdown items for overflow menu */}
            <OverflowMenuItem isPersistent>
              <Button
                id="al-filter-overflow-button"
                variant={ButtonVariant.primary}
                onClick={createAddressOnClick}
              >
                Create Address
              </Button>
            </OverflowMenuItem>
          </OverflowMenuGroup>
        </OverflowMenuContent>
        <OverflowMenuControl hasAdditionalOptions>
          <Dropdown
            id="al-filter-overflow-dropdown"
            onSelect={onKebabSelect}
            toggle={
              <KebabToggle
                id="al-filter-overflow-kebab"
                onToggle={onKebabToggle}
              />
            }
            isOpen={isKebabOpen}
            isPlain
            dropdownItems={dropdownItems}
          />
        </OverflowMenuControl>
      </OverflowMenu>
    </>
  );
};
