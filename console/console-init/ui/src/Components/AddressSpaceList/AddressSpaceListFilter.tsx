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
  filterNames: string[];
  setFilterNames: (value: Array<string>) => void;
  filterNamespaces: string[];
  setFilterNamespaces: (value: Array<string>) => void;
  filterType?: string | null;
  setFilterType: (value: string | null) => void;
  totalAddressSpaces: number;
}

interface IAddressSpaceListKebabProps {
  createAddressSpaceOnClick: () => void;
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

  const sortMenuItems = [{ key: "name", value: "Name", index: 1 }];

  const onClickSearchIcon = (event: any) => {
    if (filterValue) {
      if (filterValue === "Name") {
        if (nameSelected && nameSelected.trim() !== "" && filterNames)
          if (filterNames.indexOf(nameSelected) < 0) {
            setFilterNames([...filterNames, nameSelected]);
          }
        setNameSelected(undefined);
      } else if (filterValue === "Namespace") {
        if (namespaceSelected && namespaceSelected.trim() !== "" && filterNames)
          if (filterNamespaces.indexOf(namespaceSelected) < 0) {
            setFilterNamespaces([...filterNamespaces, namespaceSelected]);
          }
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
          index = filterNames.indexOf(id.toString());
          if (index >= 0) filterNames.splice(index, 1);
          setFilterNames([...filterNames]);
        }
        break;
      case "NamespaAddressSpaceListKebabce":
        if (filterNamespaces && id) {
          index = filterNamespaces.indexOf(id.toString());
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
    if (value.trim().length < 6) {
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

  const onChangeNamespaceData = async (value: string) => {
    setNamespaceOptions(undefined);
    setNameOptions(undefined);
    if (value.trim().length < 6) {
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
        setNamespaceOptions(obtainedList);
      }
    }
  };

  const onNamespaceSelectFilterChange = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
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
                chips={filterNames}
                deleteChip={onDelete}
                categoryName="Name"
              >
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
                      id="al-filter-search-name"
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
                chips={filterNamespaces}
                deleteChip={onDelete}
                categoryName="Namespace"
              >
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
                      selections={namespaceSelected}
                      onFilter={onNamespaceSelectFilterChange}
                      isExpanded={isSelectNamespaceExpanded}
                      ariaLabelledBy={"typeahead-select-id"}
                      placeholderText="Select Namespace"
                      isDisabled={false}
                      isCreatable={false}
                    >
                      {namespaceOptions &&
                        namespaceOptions.map((option, index) => (
                          <SelectOption key={index} value={option} />
                        ))}
                      {/* {} */}
                    </Select>
                    <Button
                      id="al-filter-search-namespace"
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
                chips={filterType ? [filterType] : []}
                deleteChip={onDelete}
                categoryName="Type"
              >
                {filterValue && filterValue === "Type" && (
                  <InputGroup>
                    <Dropdown
                      id="al-filter-dropdown-type"
                      position="left"
                      onSelect={onTypeFilterSelect}
                      isOpen={typeFilterIsExpanded}
                      toggle={
                        <DropdownToggle onToggle={setTypeFilterIsExpanded}>
                          <FilterIcon />
                          &nbsp;
                          {filterType && filterType.trim() !== ""
                            ? filterType
                            : "Type"}
                        </DropdownToggle>
                      }
                      dropdownItems={typeFilterMenuItems.map(option => (
                        <DropdownItem
                          id={`al-filter-dropdown-item-type${option.key}`}
                          key={option.key}
                          value={option.value}
                          itemID={option.key}
                          component={"button"}
                        >
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
      breakpoint="xl"
    >
      {toggleGroupItems}
    </DataToolbarToggleGroup>
  );
};

export const AddressSpaceListKebab: React.FunctionComponent<IAddressSpaceListKebabProps> = ({
  createAddressSpaceOnClick
}) => {
  const [isKebabOpen, setIsKebabOpen] = React.useState(false);

  const dropdownItems = [
    <DropdownItem key="delete-all" onClick={() => {}}>
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

  const onKebabSelect = (event: any) => {
    setIsKebabOpen(isKebabOpen);
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
                onClick={createAddressSpaceOnClick}
              >
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
