/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  SelectOption,
  SelectOptionObject,
  DataToolbarChipGroup,
  DataToolbarChip
} from "@patternfly/react-core";
import { ISortBy } from "@patternfly/react-table";
import { useApolloClient } from "@apollo/react-hooks";
import {
  FetchPolicy,
  TYPEAHEAD_REQUIRED_LENGTH,
  TypeAheadMessage
} from "constant";
import { getSelectOptionList, initalSelectOption } from "utils";
import {
  RETURN_ALL_NAMES_OF_ADDRESS_LINK_FOR_TYPEAHEAD_SEARCH,
  RETURN_ALL_CONTAINER_IDS_OF_ADDRESS_LINKS_FOR_TYPEAHEAD_SEARCH
} from "graphql-module/queries";
import {
  ISearchAddressLinkNameResponse,
  ISearchAddressLinkContainerResponse
} from "schema/ResponseTypes";
import { AddressLinksToolbar } from "modules/address-detail/components";

export interface IAddressLinksToolbarContainerProps {
  selectedNames: any[];
  setSelectedNames: (value: Array<any>) => void;
  selectedContainers: any[];
  setSelectedContainers: (value: Array<any>) => void;
  roleSelected?: string | null;
  setRoleSelected: (value: string | null) => void;
  totalRecords: number;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  namespace: string;
  addressName: string;
}

export const AddressLinksToolbarContainer: React.FunctionComponent<IAddressLinksToolbarContainerProps> = ({
  selectedNames,
  setSelectedNames,
  selectedContainers,
  setSelectedContainers,
  roleSelected,
  setRoleSelected,
  totalRecords,
  sortValue,
  setSortValue,
  namespace,
  addressName
}) => {
  const client = useApolloClient();
  const [nameSelected, setNameSelected] = useState<string>();
  const [nameInput, setNameInput] = useState<string>();
  const [containerSelected, setContainerSelected] = useState<string>();
  const [containerInput, setContainerInput] = useState<string>();
  const [nameOptions, setNameOptions] = useState<any[]>();
  const [containerOptions, setContainerOptions] = useState<any[]>();
  const [roleIsExpanded, setRoleIsExpanded] = useState<boolean>(false);
  const [filterSelected, setFilterSelected] = useState<string>("Name");

  const onClearAllFilters = () => {
    setFilterSelected("Name");
    setSelectedContainers([]);
    setSelectedNames([]);
    setRoleSelected(null);
  };

  if (!nameOptions) {
    setNameOptions([initalSelectOption]);
  }
  if (!containerOptions) {
    setContainerOptions([initalSelectOption]);
  }

  const onFilterSelect = (value: string) => {
    setFilterSelected(value);
  };
  const onNameSelect = (e: any, selection: SelectOptionObject) => {
    setNameSelected(selection.toString());
    setNameInput(undefined);
  };
  const onNameClear = () => {
    setNameSelected(undefined);
    setNameInput(undefined);
  };
  const onChangeNameInput = async (value: string) => {
    const response = await client.query<ISearchAddressLinkNameResponse>({
      query: RETURN_ALL_NAMES_OF_ADDRESS_LINK_FOR_TYPEAHEAD_SEARCH(
        addressName,
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
      response.data.addresses.addresses.length > 0 &&
      response.data.addresses.addresses[0].links &&
      response.data.addresses.addresses[0].links.links &&
      response.data.addresses.addresses[0].links.links.length > 0
    ) {
      let obtainedList = [];
      obtainedList = response.data.addresses.addresses[0].links.links.map(
        (link: any) => {
          return link.metadata.name;
        }
      );
      //get list of unique records to display in the select dropdown based on total records and 100 fetched objects
      const filteredNameOptions = getSelectOptionList(
        obtainedList,
        response.data.addresses.addresses[0].links.total
      );
      if (filteredNameOptions.length > 0) return filteredNameOptions;
    }
  };
  const onNameFilter = (e: any) => {
    const input = e.target.value && e.target.value.trim();
    setNameInput(input);
    setNameOptions(undefined);
    if (input.trim().length < TYPEAHEAD_REQUIRED_LENGTH) {
      setNameOptions([
        <SelectOption
          value={TypeAheadMessage.MORE_CHAR_REQUIRED}
          isDisabled={true}
        />
      ]);
    } else {
      onChangeNameInput(input).then(data => {
        const list = data;
        const options = list
          ? list.map((object, index) => (
              <SelectOption
                disabled={object.isDisabled}
                key={index}
                value={object.value}
              />
            ))
          : [];
        if (options && options.length > 0) {
          setNameOptions(options);
        } else {
          setNameOptions([
            <SelectOption
              value={TypeAheadMessage.NO_RESULT_FOUND}
              isDisabled={true}
            />
          ]);
        }
      });
    }
    const options = [
      <SelectOption
        value={TypeAheadMessage.MORE_CHAR_REQUIRED}
        key="1"
        isDisabled={true}
      />
    ];
    return options;
  };
  const onContainerSelect = (e: any, selection: SelectOptionObject) => {
    setContainerSelected(selection.toString());
  };
  const onContainerClear = () => {
    setContainerSelected(undefined);
    setContainerInput(undefined);
  };

  const onChangeContainerInput = async (value: string) => {
    const response = await client.query<ISearchAddressLinkContainerResponse>({
      query: RETURN_ALL_CONTAINER_IDS_OF_ADDRESS_LINKS_FOR_TYPEAHEAD_SEARCH(
        addressName,
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
      response.data.addresses.addresses.length > 0 &&
      response.data.addresses.addresses[0].links &&
      response.data.addresses.addresses[0].links.links &&
      response.data.addresses.addresses[0].links.links.length > 0
    ) {
      let obtainedList = [];
      obtainedList = response.data.addresses.addresses[0].links.links.map(
        (link: any) => {
          return link.spec.connection.spec.containerId;
        }
      );
      //get list of unique records to display in the select dropdown based on total records and 100 fetched objects
      const filteredContainersOptions = getSelectOptionList(
        obtainedList,
        response.data.addresses.addresses[0].links.total
      );
      if (filteredContainersOptions.length > 0)
        return filteredContainersOptions;
    }
  };

  const onContainerFilter = (e: any) => {
    const input = e.target.value && e.target.value.trim();
    setContainerInput(input);
    setContainerOptions(undefined);
    if (input.trim().length < TYPEAHEAD_REQUIRED_LENGTH) {
      setContainerOptions([
        <SelectOption
          value={TypeAheadMessage.MORE_CHAR_REQUIRED}
          isDisabled={true}
        />
      ]);
    } else {
      onChangeContainerInput(input).then(data => {
        const list = data;
        const options = list
          ? list.map((object, index) => (
              <SelectOption
                disabled={object.isDisabled}
                key={index}
                value={object.value}
              />
            ))
          : [];
        if (options && options.length > 0) {
          setContainerOptions(options);
        } else {
          setContainerOptions([
            <SelectOption
              value={TypeAheadMessage.NO_RESULT_FOUND}
              isDisabled={true}
            />
          ]);
        }
      });
    }
    const options = [
      <SelectOption
        value={TypeAheadMessage.MORE_CHAR_REQUIRED}
        key="1"
        isDisabled={true}
      />
    ];
    return options;
  };

  const onRoleToggle = () => {
    setRoleIsExpanded(!roleIsExpanded);
  };
  const onRoleSelect = (e: any, selection: SelectOptionObject) => {
    setRoleSelected(selection.toString());
    setRoleIsExpanded(false);
  };

  const onSearch = () => {
    if (filterSelected) {
      if (filterSelected.toLowerCase() === "name") {
        if (nameSelected && nameSelected.trim() !== "" && selectedNames)
          if (
            selectedNames.map(filter => filter.value).indexOf(nameSelected) < 0
          )
            setSelectedNames([
              ...selectedNames,
              { value: nameSelected.trim(), isExact: true }
            ]);
        if (!nameSelected && nameInput && nameInput.trim() !== "")
          if (
            selectedNames
              .map(filter => filter.value)
              .indexOf(nameInput.trim()) < 0
          )
            setSelectedNames([
              ...selectedNames,
              { value: nameInput.trim(), isExact: false }
            ]);
        setNameSelected(undefined);
      } else if (filterSelected.toLowerCase() === "container") {
        if (
          containerSelected &&
          containerSelected.trim() !== "" &&
          selectedContainers
        )
          if (
            selectedContainers
              .map(filter => filter.value)
              .indexOf(containerSelected) < 0
          ) {
            setSelectedContainers([
              ...selectedContainers,
              { value: containerSelected.trim(), isExact: true }
            ]);
          }
        if (
          !containerSelected &&
          containerInput &&
          containerInput.trim() !== ""
        )
          if (
            selectedContainers
              .map(filter => filter.value)
              .indexOf(containerInput.trim()) < 0
          )
            setSelectedContainers([
              ...selectedContainers,
              { value: containerInput.trim(), isExact: false }
            ]);
        setContainerSelected(undefined);
      }
    }
    setNameInput(undefined);
    setNameSelected(undefined);
    setContainerInput(undefined);
    setContainerSelected(undefined);
  };
  const onDelete = (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => {
    let index;
    switch (category && category.toString().toLocaleLowerCase()) {
      case "name":
        if (selectedNames && chip) {
          index = selectedNames
            .map(filter => filter.value)
            .indexOf(chip.toString());
          if (index >= 0) selectedNames.splice(index, 1);
          setSelectedNames([...selectedNames]);
        }
        break;
      case "container":
        if (selectedContainers && chip) {
          index = selectedContainers
            .map(filter => filter.value)
            .indexOf(chip.toString());
          if (index >= 0) selectedContainers.splice(index, 1);
          setSelectedContainers([...selectedContainers]);
        }
        setSelectedContainers([...selectedContainers]);
        break;
      case "role":
        setRoleSelected(null);
        break;
    }
  };

  return (
    <AddressLinksToolbar
      totalRecords={totalRecords}
      filterSelected={filterSelected}
      nameSelected={nameSelected}
      nameInput={nameInput}
      containerSelected={containerSelected}
      containerInput={containerInput}
      nameOptions={nameOptions}
      containerOptions={containerOptions}
      roleIsExpanded={roleIsExpanded}
      roleSelected={roleSelected}
      selectedNames={selectedNames}
      selectedContainers={selectedContainers}
      onFilterSelect={onFilterSelect}
      onNameSelect={onNameSelect}
      onNameClear={onNameClear}
      onNameFilter={onNameFilter}
      onContainerSelect={onContainerSelect}
      onContainerClear={onContainerClear}
      onContainerFilter={onContainerFilter}
      onRoleToggle={onRoleToggle}
      onRoleSelect={onRoleSelect}
      onSearch={onSearch}
      onDelete={onDelete}
      sortValue={sortValue}
      setSortValue={setSortValue}
      onClearAllFilters={onClearAllFilters}
    />
  );
};
