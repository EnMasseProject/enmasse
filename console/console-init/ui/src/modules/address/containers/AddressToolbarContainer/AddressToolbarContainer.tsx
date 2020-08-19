/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  SelectOptionObject,
  ToolbarChipGroup,
  ToolbarChip
} from "@patternfly/react-core";
import { ISortBy } from "@patternfly/react-table";
import { useApolloClient } from "@apollo/react-hooks";
import { FetchPolicy } from "constant";
import { getSelectOptionList } from "utils";
import { RETURN_ALL_ADDRESS_NAMES_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH } from "graphql-module/queries";
import { IAddressListNameSearchResponse } from "schema/ResponseTypes";
import { AddressToolbar } from "modules/address/components";
import { useStoreContext, MODAL_TYPES, types } from "context-state-reducer";

export interface IAddressToolbarContainerProps {
  selectedNames: any[];
  setSelectedNames: (value: Array<any>) => void;
  typeSelected?: string | null;
  setTypeSelected: (value: string | null) => void;
  statusSelected?: string | null;
  setStatusSelected: (value: string | null) => void;
  totalRecords: number;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  namespace: string;
  addressspaceName: string;
  addressspaceType?: string;
  onDeleteAllAddress: () => void;
  onPurgeAllAddress: () => void;
  isDeleteAllDisabled: boolean;
  isPurgeAllDisabled: boolean;
}

export const AddressToolbarContainer: React.FunctionComponent<IAddressToolbarContainerProps> = ({
  selectedNames,
  setSelectedNames,
  typeSelected,
  setTypeSelected,
  statusSelected,
  setStatusSelected,
  totalRecords,
  sortValue,
  setSortValue,
  namespace,
  addressspaceName,
  addressspaceType,
  onDeleteAllAddress,
  onPurgeAllAddress,
  isDeleteAllDisabled,
  isPurgeAllDisabled
}) => {
  const client = useApolloClient();
  const { dispatch } = useStoreContext();
  const [nameSelected, setNameSelected] = useState<string>();
  const [nameInput, setNameInput] = useState<string>();
  const [filterSelected, setFilterSelected] = useState<string>("Name");

  const onClearAllFilters = () => {
    setFilterSelected("Name");
    setSelectedNames([]);
    setTypeSelected(null);
    setStatusSelected(null);
  };

  //this function used to clear value of type ahead select input field on filter change
  const resetInitialState = () => {
    setNameInput("");
  };

  const onFilterSelect = (value: string) => {
    setFilterSelected(value);
    resetInitialState();
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
      const filteredNameOptions = getSelectOptionList(
        obtainedList,
        response.data.addresses.total
      );
      if (filteredNameOptions.length > 0) return filteredNameOptions;
    }
  };

  const onTypeSelect = (selection: string) => {
    setTypeSelected(selection);
  };

  const onStatusSelect = (selection: string) => {
    setStatusSelected(selection);
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
      }
    }
    setNameInput(undefined);
    setNameSelected(undefined);
  };

  const onDelete = (
    category: string | ToolbarChipGroup,
    chip: string | ToolbarChip
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
      case "type":
        setTypeSelected(null);
        break;
      case "status":
        setStatusSelected(null);
        break;
    }
  };

  const onCreateAddress = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.CREATE_ADDRESS,
      modalProps: {
        name: addressspaceName,
        namespace: namespace,
        addressSpaceType: addressspaceType
      }
    });
  };

  return (
    <AddressToolbar
      totalRecords={totalRecords}
      filterSelected={filterSelected}
      nameSelected={nameSelected}
      nameInput={nameInput}
      typeSelected={typeSelected}
      statusSelected={statusSelected}
      selectedNames={selectedNames}
      onFilterSelect={onFilterSelect}
      onNameSelect={onNameSelect}
      onNameClear={onNameClear}
      onTypeSelect={onTypeSelect}
      onStatusSelect={onStatusSelect}
      onSearch={onSearch}
      onDelete={onDelete}
      sortValue={sortValue}
      setSortValue={setSortValue}
      onClearAllFilters={onClearAllFilters}
      onDeleteAllAddress={onDeleteAllAddress}
      onPurgeAllAddress={onPurgeAllAddress}
      isDeleteAllDisabled={isDeleteAllDisabled}
      isPurgeAllDisabled={isPurgeAllDisabled}
      onClickCreateAddress={onCreateAddress}
      namespace={namespace}
      onChangeNameInput={onChangeNameInput}
      setNameInput={setNameInput}
    />
  );
};
