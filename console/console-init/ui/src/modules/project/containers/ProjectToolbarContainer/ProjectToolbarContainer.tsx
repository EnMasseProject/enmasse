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
import { RETURN_ALL_ADDRESS_SPACES_FOR_NAME_OR_NAMESPACE } from "graphql-module/queries";
import { ISearchNameOrNameSpaceAddressSpaceListResponse } from "schema/ResponseTypes";
import { IProjectFilter } from "modules/project/ProjectPage";
import { initialiseFilterForProject } from "modules/project/utils";
import { ProjectToolbar } from "modules/project/components/ProjectToolbar";

export interface IProjectToolbarContainerProps {
  filter: IProjectFilter;
  setFilter: (filter: IProjectFilter) => void;
  totalProjects: number;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onDeleteAll: () => void;
  isDeleteAllDisabled: boolean;
  onSelectAllProjects: (val: boolean) => void;
  isAllProjectSelected: boolean;
}

export const ProjectToolbarContainer: React.FunctionComponent<IProjectToolbarContainerProps> = ({
  filter,
  setFilter,
  totalProjects,
  sortValue,
  setSortValue,
  onDeleteAll,
  isDeleteAllDisabled,
  onSelectAllProjects,
  isAllProjectSelected
}) => {
  const client = useApolloClient();
  // const { dispatch } = useStoreContext();
  const [nameSelected, setNameSelected] = useState<string>();
  const [nameInput, setNameInput] = useState<string>();
  const [namespaceSelected, setNamespaceSelected] = useState<string>();
  const [namespaceInput, setNamespaceInput] = useState<string>();

  const onClearAllFilters = () => {
    setFilter(initialiseFilterForProject());
  };

  const onSelectDeleteAll = async (event: any) => {
    if (event.target.value === "deleteAll") {
      await onDeleteAll();
    }
  };

  //this function used to clear value of type ahead select input field on filter change
  const resettInitialState = () => {
    setNameSelected(undefined);
    setNameInput(undefined);
    setNamespaceInput(undefined);
    setNamespaceSelected(undefined);
  };

  const onFilterSelect = (value: string) => {
    setFilter({ ...filter, filterType: value });
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
    setFilter({ ...filter, type: selection });
  };

  const onSearch = () => {
    const { filterType, names, namespaces } = filter;
    if (filterType) {
      if (filterType.toLowerCase() === "name") {
        if (nameSelected && nameSelected.trim() !== "" && names)
          if (names.map(filter => filter.value).indexOf(nameSelected) < 0)
            setFilter({
              ...filter,
              names: [...names, { value: nameSelected.trim(), isExact: true }]
            });
        if (!nameSelected && nameInput && nameInput.trim() !== "")
          if (names.map(filter => filter.value).indexOf(nameInput.trim()) < 0)
            setFilter({
              ...filter,
              names: [...names, { value: nameInput.trim(), isExact: false }]
            });
        setNameSelected(undefined);
      } else if (filterType.toLowerCase() === "namespace") {
        if (namespaceSelected && namespaceSelected.trim() !== "" && namespaces)
          if (
            namespaces.map(filter => filter.value).indexOf(namespaceSelected) <
            0
          ) {
            setFilter({
              ...filter,
              namespaces: [
                ...namespaces,
                { value: namespaceSelected.trim(), isExact: true }
              ]
            });
          }
        if (
          !namespaceSelected &&
          namespaceInput &&
          namespaceInput.trim() !== ""
        )
          if (
            namespaces
              .map(filter => filter.value)
              .indexOf(namespaceInput.trim()) < 0
          )
            setFilter({
              ...filter,
              namespaces: [
                ...namespaces,
                { value: namespaceInput.trim(), isExact: false }
              ]
            });
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
    const { names, namespaces } = filter;
    let index: number;
    switch (category && category.toString().toLowerCase()) {
      case "name":
        if (names && chip) {
          index = names.map(filter => filter.value).indexOf(chip.toString());
          if (index >= 0) names.splice(index, 1);
          setFilter({ ...filter, names: [...names] });
        }
        break;
      case "namespace":
        if (namespaces && chip) {
          index = namespaces
            .map(filter => filter.value)
            .indexOf(chip.toString());
          if (index >= 0) namespaces.splice(index, 1);
          setFilter({ ...filter, namespaces: [...namespaces] });
        }
        break;
      case "type":
        setFilter({ ...filter, type: undefined });
        break;
    }
  };

  return (
    <ProjectToolbar
      totalRecords={totalProjects}
      filterSelected={filter.filterType}
      nameSelected={nameSelected}
      nameInput={nameInput}
      namespaceSelected={namespaceSelected}
      namespaceInput={namespaceInput}
      typeSelected={filter.type}
      selectedNames={filter.names}
      selectedNamespaces={filter.namespaces}
      onFilterSelect={onFilterSelect}
      onNameSelect={onNameSelect}
      onNameClear={onNameClear}
      onNamespaceSelect={onNamespaceSelect}
      onNamespaceClear={onNamespaceClear}
      onTypeSelect={onTypeSelect}
      onDeleteAll={onDeleteAll}
      onSearch={onSearch}
      onDelete={onDelete}
      isDeleteAllDisabled={isDeleteAllDisabled}
      onSelectDeleteAll={onSelectDeleteAll}
      sortValue={sortValue}
      setSortValue={setSortValue}
      onClearAllFilters={onClearAllFilters}
      onChangeNameInput={onChangeNameInput}
      onChangeNameSpaceInput={onChangeNamespaceInput}
      setNameInput={setNameInput}
      setNameSpaceInput={setNamespaceInput}
      onSelectAllProjects={onSelectAllProjects}
      isAllProjectSelected={isAllProjectSelected}
    />
  );
};
