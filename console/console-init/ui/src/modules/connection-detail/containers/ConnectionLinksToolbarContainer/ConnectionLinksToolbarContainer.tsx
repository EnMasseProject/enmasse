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
  RETURN_ALL_CONNECTION_LINKS_FOR_NAME_SEARCH,
  RETURN_ALL_CONNECTION_LINKS_FOR_ADDRESS_SEARCH
} from "graphql-module/queries";
import {
  IConnectionLinksNameSearchResponse,
  IConnectionLinksAddressSearchResponse
} from "schema/ResponseTypes";
import { ConnectionLinksToolbar } from "modules/connection-detail/components";

export interface IConnectionLinksToolbarContainerProps {
  selectedNames: any[];
  setSelectedNames: (value: Array<any>) => void;
  selectedAddresses: any[];
  setSelectedAddresses: (value: Array<any>) => void;
  roleSelected?: string | null;
  setRoleSelected: (value: string | null) => void;
  totalRecords: number;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  namespace: string;
  connectionName: string;
}

export const ConnectionLinksToolbarContainer: React.FunctionComponent<IConnectionLinksToolbarContainerProps> = ({
  selectedNames,
  setSelectedNames,
  selectedAddresses,
  setSelectedAddresses,
  roleSelected,
  setRoleSelected,
  totalRecords,
  sortValue,
  setSortValue,
  namespace,
  connectionName
}) => {
  const client = useApolloClient();
  const [nameSelected, setNameSelected] = useState<string>();
  const [nameInput, setNameInput] = useState<string>();
  const [addressSelected, setAddressSelected] = useState<string>();
  const [addressInput, setAddressInput] = useState<string>();
  const [nameOptions, setNameOptions] = useState<any[] | undefined>();
  const [addressOptions, setAddressOptions] = useState<any[] | undefined>([]);
  const [roleIsExpanded, setRoleIsExpanded] = useState<boolean>(false);
  const [filterSelected, setFilterSelected] = useState<string>("Name");

  const onClearAllFilters = () => {
    setFilterSelected("Name");
    setSelectedAddresses([]);
    setSelectedNames([]);
    setRoleSelected(null);
  };

  if (!nameOptions) {
    setNameOptions([initalSelectOption]);
  }
  if (!addressOptions) {
    setAddressOptions([initalSelectOption]);
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
      const filteredNameOptions = getSelectOptionList(
        obtainedList,
        response.data.connections.connections[0].links.total
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
  const onAddressSelect = (e: any, selection: SelectOptionObject) => {
    setAddressSelected(selection.toString());
  };
  const onAddressClear = () => {
    setAddressSelected(undefined);
    setAddressInput(undefined);
  };

  const onChangeAddressInput = async (value: string) => {
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
      const filteredAddressOptions = getSelectOptionList(
        obtainedList,
        response.data.connections.connections[0].links.total
      );
      if (filteredAddressOptions.length > 0) return filteredAddressOptions;
    }
  };

  const onAddressFilter = (e: any) => {
    const input = e.target.value && e.target.value.trim();
    setAddressInput(input);
    setAddressOptions(undefined);
    if (input.trim().length < TYPEAHEAD_REQUIRED_LENGTH) {
      setAddressOptions([
        <SelectOption
          value={TypeAheadMessage.MORE_CHAR_REQUIRED}
          isDisabled={true}
        />
      ]);
    } else {
      onChangeAddressInput(input).then(data => {
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
          setAddressOptions(options);
        } else {
          setAddressOptions([
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
      } else if (filterSelected.toLowerCase() === "address") {
        if (
          addressSelected &&
          addressSelected.trim() !== "" &&
          selectedAddresses
        )
          if (
            selectedAddresses
              .map(filter => filter.value)
              .indexOf(addressSelected) < 0
          ) {
            setSelectedAddresses([
              ...selectedAddresses,
              { value: addressSelected.trim(), isExact: true }
            ]);
          }
        if (!addressSelected && addressInput && addressInput.trim() !== "")
          if (
            selectedAddresses
              .map(filter => filter.value)
              .indexOf(addressInput.trim()) < 0
          )
            setSelectedAddresses([
              ...selectedAddresses,
              { value: addressInput.trim(), isExact: false }
            ]);
        setAddressSelected(undefined);
      }
    }
    setNameInput(undefined);
    setNameSelected(undefined);
    setAddressInput(undefined);
    setAddressSelected(undefined);
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
      case "address":
        if (selectedAddresses && chip) {
          index = selectedAddresses
            .map(filter => filter.value)
            .indexOf(chip.toString());
          if (index >= 0) selectedAddresses.splice(index, 1);
          setSelectedAddresses([...selectedAddresses]);
        }
        setSelectedAddresses([...selectedAddresses]);
        break;
      case "role":
        setRoleSelected(null);
        break;
    }
  };

  return (
    <ConnectionLinksToolbar
      totalRecords={totalRecords}
      filterSelected={filterSelected}
      nameSelected={nameSelected}
      nameInput={nameInput}
      addressSelected={addressSelected}
      addressInput={addressInput}
      nameOptions={nameOptions}
      addressOptions={addressOptions}
      roleIsExpanded={roleIsExpanded}
      roleSelected={roleSelected}
      selectedNames={selectedNames}
      selectedAddresses={selectedAddresses}
      onFilterSelect={onFilterSelect}
      onNameSelect={onNameSelect}
      onNameClear={onNameClear}
      onNameFilter={onNameFilter}
      onAddressSelect={onAddressSelect}
      onAddressClear={onAddressClear}
      onAddressFilter={onAddressFilter}
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
