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
  const [nameOptions, setNameOptions] = useState<any[] | undefined>();
  const [namespaceOptions, setNamespaceOptions] = useState<any[] | undefined>(
    []
  );
  const [typeIsExpanded, setTypeIsExpanded] = useState<boolean>(false);

  const [filterSelected, setFilterSelected] = useState<string>("Name");

  const onClearAllFilters = () => {
    setFilterSelected("Name");
    setSelectedNamespaces([]);
    setSelectedNames([]);
    setTypeSelected(null);
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

  if (!nameOptions) {
    setNameOptions([initalSelectOption]);
  }
  if (!namespaceOptions) {
    setNamespaceOptions([initalSelectOption]);
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
    const response = await client.query<
      ISearchNameOrNameSpaceAddressSpaceListResponse
    >({
      query: RETURN_ALL_ADDRESS_SPACES_FOR_NAME_OR_NAMESPACE(
        true,
        value.trim()
      ),
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    if (
      response &&
      response.data &&
      response.data.addressSpaces &&
      response.data.addressSpaces.addressSpaces &&
      response.data.addressSpaces.addressSpaces.length > 0
    ) {
      let obtainedList = response.data.addressSpaces.addressSpaces.map(
        (link: any) => {
          return link.metadata.name;
        }
      );
      //get list of unique records to display in the select dropdown based on total records and 100 fetched objects
      const filteredNameOptions = getSelectOptionList(
        obtainedList,
        response.data.addressSpaces.total
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
  const onNamespaceSelect = (e: any, selection: SelectOptionObject) => {
    setNamespaceSelected(selection.toString());
  };
  const onNamespaceClear = () => {
    setNamespaceSelected(undefined);
    setNamespaceInput(undefined);
  };

  const onChangeNamespaceInput = async (value: string) => {
    const response = await client.query<
      ISearchNameOrNameSpaceAddressSpaceListResponse
    >({
      query: RETURN_ALL_ADDRESS_SPACES_FOR_NAME_OR_NAMESPACE(
        false,
        value.trim()
      ),
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    if (
      response &&
      response.data &&
      response.data.addressSpaces &&
      response.data.addressSpaces.addressSpaces &&
      response.data.addressSpaces.addressSpaces.length > 0
    ) {
      let obtainedList = response.data.addressSpaces.addressSpaces.map(
        (link: any) => {
          return link.metadata.namespace;
        }
      );
      //get list of unique records to display in the select dropdown based on total records and 100 fetched objects
      const filteredNamespaceOptions = getSelectOptionList(
        obtainedList,
        response.data.addressSpaces.total
      );
      if (filteredNamespaceOptions.length > 0) return filteredNamespaceOptions;
    }
  };

  const onNamespaceFilter = (e: any) => {
    const input = e.target.value && e.target.value.trim();
    setNamespaceInput(input);
    setNamespaceOptions(undefined);
    if (input.trim().length < TYPEAHEAD_REQUIRED_LENGTH) {
      setNamespaceOptions([
        <SelectOption
          value={TypeAheadMessage.MORE_CHAR_REQUIRED}
          isDisabled={true}
        />
      ]);
    } else {
      onChangeNamespaceInput(input).then(data => {
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
          setNamespaceOptions(options);
        } else {
          setNamespaceOptions([
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

  const onTypeToggle = () => {
    setTypeIsExpanded(!typeIsExpanded);
  };
  const onTypeSelect = (e: any, selection: SelectOptionObject) => {
    setTypeSelected(selection.toString());
    setTypeIsExpanded(false);
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
      nameOptions={nameOptions}
      namespaceOptions={namespaceOptions}
      typeIsExpanded={typeIsExpanded}
      typeSelected={typeSelected}
      selectedNames={selectedNames}
      selectedNamespaces={selectedNamespaces}
      onFilterSelect={onFilterSelect}
      onNameSelect={onNameSelect}
      onNameClear={onNameClear}
      onNameFilter={onNameFilter}
      onNamespaceSelect={onNamespaceSelect}
      onNamespaceClear={onNamespaceClear}
      onNamespaceFilter={onNamespaceFilter}
      onTypeToggle={onTypeToggle}
      onTypeSelect={onTypeSelect}
      onDeleteAll={onDeleteAll}
      onSearch={onSearch}
      onDelete={onDelete}
      onCreateAddressSpace={onCreateAddressSpace}
      isDeleteAllDisabled={isDeleteAllDisabled}
      onSelectDeleteAll={onSelectDeleteAll}
      sortValue={sortValue}
      setSortValue={setSortValue}
      onClearAllFilters={onClearAllFilters}
    />
  );
};
