/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  SelectOptionObject,
  DataToolbarChipGroup,
  DataToolbarChip
} from "@patternfly/react-core";
import { ISortBy } from "@patternfly/react-table";
import { useApolloClient } from "@apollo/react-hooks";
import { FetchPolicy } from "constant";
import { getSelectOptionList } from "utils";
import { types, MODAL_TYPES, useStoreContext } from "context-state-reducer";
import { RETURN_ALL_ADDRESS_SPACES_FOR_NAME_OR_NAMESPACE } from "graphql-module/queries";
import { ISearchNameOrNameSpaceAddressSpaceListResponse } from "schema/ResponseTypes";
import { MessagingToolbar } from "modules/address-space/components";

export interface IMessagingToolbarContainerProps {
  selectedNames: any[];
  setSelectedNames: (value: Array<any>) => void;
  selectedNamespaces: any[];
  setSelectedNamespaces: (value: Array<any>) => void;
  typeSelected?: string | null;
  setTypeSelected: (value: string | null) => void;
  statusSelected?: string | null;
  setStatusSelected: (value: string | null) => void;
  totalAddressSpaces: number;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onDeleteAll: () => void;
  isDeleteAllDisabled: boolean;
}

export const MessagingToolbarContainer: React.FunctionComponent<IMessagingToolbarContainerProps> = ({
  selectedNames,
  setSelectedNames,
  selectedNamespaces,
  setSelectedNamespaces,
  typeSelected,
  setTypeSelected,
  statusSelected,
  setStatusSelected,
  totalAddressSpaces,
  sortValue,
  setSortValue,
  onDeleteAll,
  isDeleteAllDisabled
}) => {
  const client = useApolloClient();
  const { dispatch } = useStoreContext();
  const [nameSelected, setNameSelected] = useState<string>();
  const [nameInput, setNameInput] = useState<string>();
  const [namespaceSelected, setNamespaceSelected] = useState<string>();
  const [namespaceInput, setNamespaceInput] = useState<string>();
  const [filterSelected, setFilterSelected] = useState<string>("Name");

  const onClearAllFilters = () => {
    setFilterSelected("Name");
    setSelectedNamespaces([]);
    setSelectedNames([]);
    setTypeSelected(null);
    setStatusSelected(null);
  };

  const onCreateAddressSpace = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.CREATE_ADDRESS_SPACE
    });
  };

  const onSelectDeleteAll = async (event: any) => {
    if (event.target.value === "deleteAll") {
      await onDeleteAll();
    }
  };

  //this function used to clear value of type ahead select input field on filter change
  const resettInitialState = () => {
    setNameInput("");
    setNamespaceInput("");
    setTypeSelected(null);
    setStatusSelected(null);
  };

  const onFilterSelect = (value: string) => {
    setFilterSelected(value);
    resettInitialState();
  };

  const onNameSelect = (e: any, selection: SelectOptionObject) => {
    setNameSelected(selection.toString());
    setNameInput(undefined);
  };

  const onNameClear = () => {
    setNameSelected(undefined);
    setNameInput(undefined);
  };

  const getAddressSpaceForNameORNameSpace = async (
    value: string,
    propertyName: string
  ) => {
    const response = await client.query<
      ISearchNameOrNameSpaceAddressSpaceListResponse
    >({
      query: RETURN_ALL_ADDRESS_SPACES_FOR_NAME_OR_NAMESPACE(
        propertyName,
        value.trim()
      ),
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    return response;
  };

  const getOptions = (
    response: ISearchNameOrNameSpaceAddressSpaceListResponse,
    propertyname: string
  ) => {
    if (
      response.addressSpaces &&
      response.addressSpaces.addressSpaces &&
      response.addressSpaces.addressSpaces.length > 0
    ) {
      let obtainedList = response.addressSpaces.addressSpaces.map(
        (link: any) => link.metadata[propertyname]
      );
      //get list of unique records to display in the select dropdown based on total records and 100 fetched objects
      const filteredNameOptions = getSelectOptionList(
        obtainedList,
        response.addressSpaces.total
      );
      if (filteredNameOptions.length > 0) return filteredNameOptions;
    }
  };

  const onChangeNameInput = async (value: string) => {
    const response = await getAddressSpaceForNameORNameSpace(value, "name");
    return getOptions(response.data, "name");
  };

  const onNamespaceSelect = (e: any, selection: SelectOptionObject) => {
    setNamespaceSelected(selection.toString());
  };

  const onNamespaceClear = () => {
    setNamespaceSelected(undefined);
    setNamespaceInput(undefined);
  };

  const onChangeNamespaceInput = async (value: string) => {
    const response = await getAddressSpaceForNameORNameSpace(
      value,
      "namespace"
    );
    return getOptions(response.data, "namespace");
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
      } else if (filterSelected.toLowerCase() === "namespace") {
        if (
          namespaceSelected &&
          namespaceSelected.trim() !== "" &&
          selectedNamespaces
        )
          if (
            selectedNamespaces
              .map(filter => filter.value)
              .indexOf(namespaceSelected) < 0
          ) {
            setSelectedNamespaces([
              ...selectedNamespaces,
              { value: namespaceSelected.trim(), isExact: true }
            ]);
          }
        if (
          !namespaceSelected &&
          namespaceInput &&
          namespaceInput.trim() !== ""
        )
          if (
            selectedNamespaces
              .map(filter => filter.value)
              .indexOf(namespaceInput.trim()) < 0
          )
            setSelectedNamespaces([
              ...selectedNamespaces,
              { value: namespaceInput.trim(), isExact: false }
            ]);
        setNamespaceSelected(undefined);
      }
    }

    setNameInput(undefined);
    setNameSelected(undefined);
    setNamespaceInput(undefined);
    setNamespaceSelected(undefined);
  };

  const onDelete = (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => {
    let index: number;
    switch (category && category.toString().toLowerCase()) {
      case "name":
        if (selectedNames && chip) {
          index = selectedNames
            .map(filter => filter.value)
            .indexOf(chip.toString());
          if (index >= 0) selectedNames.splice(index, 1);
          setSelectedNames([...selectedNames]);
        }
        break;
      case "namespace":
        if (selectedNamespaces && chip) {
          index = selectedNamespaces
            .map(filter => filter.value)
            .indexOf(chip.toString());
          if (index >= 0) selectedNamespaces.splice(index, 1);
          setSelectedNamespaces([...selectedNamespaces]);
        }
        setSelectedNamespaces([...selectedNamespaces]);
        break;
      case "type":
        setTypeSelected(null);
        break;
      case "status":
        setStatusSelected(null);
        break;
    }
  };

  return (
    <MessagingToolbar
      totalRecords={totalAddressSpaces}
      filterSelected={filterSelected}
      nameSelected={nameSelected}
      nameInput={nameInput}
      namespaceSelected={namespaceSelected}
      namespaceInput={namespaceInput}
      typeSelected={typeSelected}
      statusSelected={statusSelected}
      selectedNames={selectedNames}
      selectedNamespaces={selectedNamespaces}
      onFilterSelect={onFilterSelect}
      onNameSelect={onNameSelect}
      onNameClear={onNameClear}
      onNamespaceSelect={onNamespaceSelect}
      onNamespaceClear={onNamespaceClear}
      onTypeSelect={onTypeSelect}
      onStatusSelect={onStatusSelect}
      onDeleteAll={onDeleteAll}
      onSearch={onSearch}
      onDelete={onDelete}
      onCreateAddressSpace={onCreateAddressSpace}
      isDeleteAllDisabled={isDeleteAllDisabled}
      onSelectDeleteAll={onSelectDeleteAll}
      sortValue={sortValue}
      setSortValue={setSortValue}
      onClearAllFilters={onClearAllFilters}
      onChangeNameInput={onChangeNameInput}
      onChangeNameSpaceInput={onChangeNamespaceInput}
      setNameInput={setNameInput}
      setNameSpaceInput={setNamespaceInput}
    />
  );
};
