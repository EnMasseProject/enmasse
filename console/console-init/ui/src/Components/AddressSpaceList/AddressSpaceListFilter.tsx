/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  DataToolbarChip,
  DataToolbarToggleGroup,
  DataToolbarGroup,
  DataToolbarFilter,
  DataToolbarItem,
  OverflowMenu,
  OverflowMenuContent,
  OverflowMenuGroup,
  OverflowMenuItem,
  OverflowMenuControl
} from "@patternfly/react-core/dist/js/experimental";
import { FilterIcon, SearchIcon } from "@patternfly/react-icons";
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  InputGroup,
  Button,
  ButtonVariant,
  Badge,
  KebabToggle,
  SelectOption,
  SelectOptionObject,
  Select,
  SelectVariant
} from "@patternfly/react-core";
import { RETURN_ALL_ADDRESS_SPACES_FOR_NAME_OR_NAMESPACE } from "src/Queries/Queries";
import { ISearchNameOrNameSpaceAddressSpaceListResponse } from "src/Types/ResponseTypes";
import { useApolloClient } from "@apollo/react-hooks";

interface IAddressSpaceListFilterProps {
  filterValue?: string;
  setFilterValue: (value: string) => void;
  filterNames: any[];
  setFilterNames: (value: Array<any>) => void;
  filterNamespaces: any[];
  setFilterNamespaces: (value: Array<any>) => void;
  filterType?: string | null;
  setFilterType: (value: string | null) => void;
  totalAddressSpaces: number;
}

interface IAddressSpaceListKebabProps {
  createAddressSpaceOnClick: () => void;
  onDeleteAll: () => void;
}
export const AddressSpaceListFilter: React.FunctionComponent<IAddressSpaceListFilterProps> = ({
  filterValue,
  setFilterValue,
  filterNames,
  setFilterNames,
  filterNamespaces,
  setFilterNamespaces,
  filterType,
  setFilterType,
  totalAddressSpaces
}) => {
  const client = useApolloClient();
  const [filterIsExpanded, setFilterIsExpanded] = React.useState<boolean>(
    false
  );
  const [typeFilterIsExpanded, setTypeFilterIsExpanded] = React.useState<
    boolean
  >(false);
  const [isSelectNameExpanded, setIsSelectNameExpanded] = React.useState<
    boolean
  >(false);
  const [
    isSelectNamespaceExpanded,
    setIsSelectNamespaceExpanded
  ] = React.useState<boolean>(false);
  const [nameSelected, setNameSelected] = React.useState<string>();
  const [namespaceSelected, setNamespaceSelected] = React.useState<string>();
  const [nameOptions, setNameOptions] = React.useState<Array<string>>();
  const [nameInput, setNameInput] = React.useState<string>("");
  const [nameSpaceInput, setNameSpaceInput] = React.useState<string>("");
  const [namespaceOptions, setNamespaceOptions] = React.useState<
    Array<string>
  >();

  const filterMenuItems = [
    { key: "filterName", value: "Name" },
    { key: "filterNamespace", value: "Namespace" },
    { key: "filterType", value: "Type" }
  ];
  const typeFilterMenuItems = [
    { key: "typeStandard", value: "Standard" },
    { key: "typeBrokered", value: "Brokered" }
  ];

  const onClickSearchIcon = (event: any) => {
    if (filterValue) {
      if (filterValue === "Name") {
        if (nameSelected && nameSelected.trim() !== "" && filterNames)
          if (filterNames.map(filter => filter.value).indexOf(nameSelected) < 0)
            setFilterNames([
              ...filterNames,
              { value: nameSelected.trim(), isExact: true }
            ]);
        if (!nameSelected && nameInput && nameInput.trim() !== "")
          if (
            filterNames.map(filter => filter.value).indexOf(nameInput.trim()) <
            0
          )
            setFilterNames([
              ...filterNames,
              { value: nameInput.trim(), isExact: false }
            ]);
        setNameSelected(undefined);
      } else if (filterValue === "Namespace") {
        if (namespaceSelected && namespaceSelected.trim() !== "" && filterNames)
          if (
            filterNamespaces
              .map(filter => filter.value)
              .indexOf(namespaceSelected) < 0
          ) {
            setFilterNamespaces([
              ...filterNamespaces,
              { value: namespaceSelected.trim(), isExact: true }
            ]);
          }
        if (
          !namespaceSelected &&
          nameSpaceInput &&
          nameSpaceInput.trim() !== ""
        )
          if (
            filterNamespaces
              .map(filter => filter.value)
              .indexOf(nameSpaceInput.trim()) < 0
          )
            setFilterNamespaces([
              ...filterNamespaces,
              { value: nameSpaceInput.trim(), isExact: false }
            ]);
        setNamespaceSelected(undefined);
      }
    }
  };

  const onDelete = (
    type: string | DataToolbarChip,
    id: string | DataToolbarChip
  ) => {
    let index;
    switch (type) {
      case "Name":
        if (filterNames && id) {
          index = filterNames
            .map(filter => filter.value)
            .indexOf(id.toString());
          if (index >= 0) filterNames.splice(index, 1);
          setFilterNames([...filterNames]);
        }
        break;
      case "Namespace":
        if (filterNamespaces && id) {
          index = filterNamespaces
            .map(filter => filter.value)
            .indexOf(id.toString());
          if (index >= 0) filterNamespaces.splice(index, 1);
          setFilterNamespaces([...filterNamespaces]);
        }
        setFilterNamespaces([...filterNamespaces]);
        break;
      case "Type":
        setFilterType(null);
        break;
    }
  };
  const onFilterSelect = (event: any) => {
    setFilterValue(event.target.value);
    setFilterIsExpanded(!filterIsExpanded);
  };

  const onTypeFilterSelect = (event: any) => {
    setFilterType(event.target.value);
    setTypeFilterIsExpanded(!typeFilterIsExpanded);
  };

  const onNameSelectToggle = () => {
    setIsSelectNameExpanded(!isSelectNameExpanded);
  };

  const onNamespaceSelectToggle = () => {
    setIsSelectNamespaceExpanded(!isSelectNamespaceExpanded);
  };

  const onChangeNameData = async (value: string) => {
    setNameOptions(undefined);
    if (value.trim().length < 5) {
      setNameOptions([]);
      return;
    }
    const response = await client.query<
      ISearchNameOrNameSpaceAddressSpaceListResponse
    >({
      query: RETURN_ALL_ADDRESS_SPACES_FOR_NAME_OR_NAMESPACE(true, value.trim())
    });
    if (
      response &&
      response.data &&
      response.data.addressSpaces &&
      response.data.addressSpaces.AddressSpaces &&
      response.data.addressSpaces.AddressSpaces.length > 0
    ) {
      if (response.data.addressSpaces.Total > 100) {
        setNameOptions([]);
      } else {
        const obtainedList = response.data.addressSpaces.AddressSpaces.map(
          (link: any) => {
            return link.ObjectMeta.Name;
          }
        );
        setNameOptions(Array.from(new Set(obtainedList)));
      }
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

  const onChangeNamespaceData = async (value: string) => {
    setNamespaceOptions(undefined);
    setNameOptions(undefined);
    if (value.trim().length < 5) {
      setNameOptions([]);
      return;
    }
    const response = await client.query<
      ISearchNameOrNameSpaceAddressSpaceListResponse
    >({
      query: RETURN_ALL_ADDRESS_SPACES_FOR_NAME_OR_NAMESPACE(
        false,
        value.trim()
      )
    });
    if (
      response &&
      response.data &&
      response.data.addressSpaces &&
      response.data.addressSpaces.AddressSpaces &&
      response.data.addressSpaces.AddressSpaces.length > 0
    ) {
      if (response.data.addressSpaces.Total > 100) {
        setNamespaceOptions([]);
      } else {
        const obtainedList = response.data.addressSpaces.AddressSpaces.map(
          (link: any) => {
            return link.ObjectMeta.Namespace;
          }
        );
        setNamespaceOptions(Array.from(new Set(obtainedList)));
      }
    }
  };

  const onNamespaceSelectFilterChange = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setNameSpaceInput(e.target.value);
    onChangeNamespaceData(e.target.value);
    const options: React.ReactElement[] = namespaceOptions
      ? namespaceOptions.map((option, index) => (
          <SelectOption key={index} value={option} />
        ))
      : [];
    return options;
  };

  const onNameSelect = (event: any, selection: string | SelectOptionObject) => {
    setNameSelected(selection.toString());
    setIsSelectNameExpanded(false);
  };

  const onNamespaceSelect = (
    event: any,
    selection: string | SelectOptionObject
  ) => {
    setNamespaceSelected(selection.toString());
    setIsSelectNamespaceExpanded(false);
  };

  const checkIsFilterApplied = () => {
    if (
      (filterNames && filterNames.length > 0) ||
      (filterNamespaces && filterNamespaces.length > 0) ||
      (filterType && filterType.trim() !== "")
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
                component={"button"}>
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
                categoryName="Name">
                {filterValue && filterValue === "Name" && (
                  <InputGroup>
                    <Select
                      id="al-filter-input-name"
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
                      isCreatable={false}>
                      {nameOptions && nameOptions.length > 0 ? (
                        nameOptions.map((option, index) => (
                          <SelectOption key={index} value={option} />
                        ))
                      ) : nameInput.trim().length < 5 ? (
                        <SelectOption
                          key={"invalid-input-length"}
                          value={"Enter more characters"}
                          disabled={true}
                        />
                      ) : (
                        <SelectOption
                          key={"no-results-found"}
                          value={"No results found"}
                          disabled={true}
                        />
                      )}
                      {/* {} */}
                    </Select>
                    <Button
                      id="al-filter-search-name"
                      variant={ButtonVariant.control}
                      aria-label="search button for search input"
                      onClick={onClickSearchIcon}>
                      <SearchIcon />
                    </Button>
                  </InputGroup>
                )}
              </DataToolbarFilter>
            </DataToolbarItem>
            <DataToolbarItem>
              <DataToolbarFilter
                chips={filterNamespaces.map(filter => filter.value)}
                deleteChip={onDelete}
                categoryName="Namespace">
                {filterValue && filterValue === "Namespace" && (
                  <InputGroup>
                    <Select
                      id="al-filter-input-namespace"
                      variant={SelectVariant.typeahead}
                      aria-label="Select a Namespace"
                      onToggle={onNamespaceSelectToggle}
                      onSelect={onNamespaceSelect}
                      onClear={() => {
                        setNamespaceSelected(undefined);
                        setIsSelectNamespaceExpanded(false);
                      }}
                      maxHeight="200px"
                      selections={namespaceSelected}
                      onFilter={onNamespaceSelectFilterChange}
                      isExpanded={isSelectNamespaceExpanded}
                      ariaLabelledBy={"typeahead-select-id"}
                      placeholderText="Select Namespace"
                      isDisabled={false}
                      isCreatable={false}>
                      {namespaceOptions && namespaceOptions.length > 0 ? (
                        namespaceOptions.map((option, index) => (
                          <SelectOption key={index} value={option} />
                        ))
                      ) : nameSpaceInput.trim().length < 5 ? (
                        <SelectOption
                          key={"invalid-input-length"}
                          value={"Enter more characters"}
                          disabled={true}
                        />
                      ) : (
                        <SelectOption
                          key={"no-results-found"}
                          value={"No results found"}
                          disabled={true}
                        />
                      )}
                      {/* {} */}
                    </Select>
                    <Button
                      id="al-filter-search-namespace"
                      variant={ButtonVariant.control}
                      aria-label="search button for search input"
                      onClick={onClickSearchIcon}>
                      <SearchIcon />
                    </Button>
                  </InputGroup>
                )}
              </DataToolbarFilter>
            </DataToolbarItem>
            <DataToolbarItem>
              <DataToolbarFilter
                chips={filterType ? [filterType] : []}
                deleteChip={onDelete}
                categoryName="Type">
                {filterValue && filterValue === "Type" && (
                  <InputGroup>
                    <Dropdown
                      id="al-filter-dropdown-type"
                      position="left"
                      onSelect={onTypeFilterSelect}
                      isOpen={typeFilterIsExpanded}
                      toggle={
                        <DropdownToggle onToggle={setTypeFilterIsExpanded}>
                          {filterType && filterType.trim() !== ""
                            ? filterType
                            : "Select Type"}
                        </DropdownToggle>
                      }
                      dropdownItems={typeFilterMenuItems.map(option => (
                        <DropdownItem
                          id={`al-filter-dropdown-item-type${option.key}`}
                          key={option.key}
                          value={option.value}
                          itemID={option.key}
                          component={"button"}>
                          {option.value}
                        </DropdownItem>
                      ))}
                    />
                  </InputGroup>
                )}
              </DataToolbarFilter>
            </DataToolbarItem>
          </>
        )}
      </DataToolbarGroup>
    </>
  );
  return (
    <DataToolbarToggleGroup
      toggleIcon={
        <>
          <FilterIcon />
          {checkIsFilterApplied() && (
            <Badge key={1} isRead>
              {totalAddressSpaces}
            </Badge>
          )}
        </>
      }
      breakpoint="xl">
      {toggleGroupItems}
    </DataToolbarToggleGroup>
  );
};

export const AddressSpaceListKebab: React.FunctionComponent<IAddressSpaceListKebabProps> = ({
  createAddressSpaceOnClick,
  onDeleteAll
}) => {
  const [isKebabOpen, setIsKebabOpen] = React.useState(false);

  const dropdownItems = [
    <DropdownItem key="delete-all" component="button" value="deleteAll">
      Delete All
    </DropdownItem>
    // <OverflowMenuDropdownItem key="secondary" isShared={true}>
    //   Create Address
    // </OverflowMenuDropdownItem>,
    // <OverflowMenuDropdownItem key="delete-all">
    //   Delete All
    // </OverflowMenuDropdownItem>
  ];
  const onKebabToggle = (isOpen: boolean) => {
    setIsKebabOpen(isOpen);
  };

  const onKebabSelect = async (event: any) => {
    if (event.target.value === "deleteAll") {
      await onDeleteAll();
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
                onClick={createAddressSpaceOnClick}>
                Create Address Space
              </Button>
            </OverflowMenuItem>
          </OverflowMenuGroup>
        </OverflowMenuContent>
        <OverflowMenuControl hasAdditionalOptions>
          <Dropdown
            id="al-filter-overflow-dropdown"
            onSelect={onKebabSelect}
            toggle={<KebabToggle onToggle={onKebabToggle} />}
            isOpen={isKebabOpen}
            isPlain
            dropdownItems={dropdownItems}
          />
        </OverflowMenuControl>
      </OverflowMenu>
    </>
  );
};
