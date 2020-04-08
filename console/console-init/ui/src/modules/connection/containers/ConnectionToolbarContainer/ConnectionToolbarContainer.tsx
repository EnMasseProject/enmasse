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
import { RETURN_ALL_CONNECTIONS_HOSTNAME_AND_CONTAINERID_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH } from "graphql-module/queries";
import { IConnectionListNameSearchResponse } from "schema/ResponseTypes";
import { ConnectionsToolbar } from "modules/connection/components";

export interface IConnectionToolbarContainerProps {
  selectedHostnames: any[];
  setSelectedHostnames: (value: Array<any>) => void;
  selectedContainers: any[];
  setSelectedContainers: (value: Array<any>) => void;
  totalRecords: number;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  namespace: string;
  addressSpaceName: string;
}

export const ConnectionToolbarContainer: React.FunctionComponent<IConnectionToolbarContainerProps> = ({
  selectedHostnames,
  setSelectedHostnames,
  selectedContainers,
  setSelectedContainers,
  totalRecords,
  sortValue,
  setSortValue,
  namespace,
  addressSpaceName
}) => {
  const client = useApolloClient();
  const [hostnameSelected, setHostnameSelected] = useState<string>();
  const [hostnameInput, setHostnameInput] = useState<string>();
  const [containerSelected, setContainerSelected] = useState<string>();
  const [containerInput, setContainerInput] = useState<string>();
  const [filterSelected, setFilterSelected] = useState<string>("Hostname");

  const onClearAllFilters = () => {
    setFilterSelected("Hostname");
    setSelectedContainers([]);
    setSelectedHostnames([]);
  };

  //this function used to clear value of type ahead select input field on filter change
  const resettInitialState = () => {
    setHostnameInput("");
    setContainerInput("");
  };

  const onFilterSelect = (value: string) => {
    setFilterSelected(value);
    resettInitialState();
  };

  const onHostnameSelect = (e: any, selection: SelectOptionObject) => {
    setHostnameSelected(selection.toString());
    setHostnameInput(undefined);
  };

  const onHostnameClear = () => {
    setHostnameSelected(undefined);
    setHostnameInput(undefined);
  };

  const onChangeHostNameInput = async (value: string) => {
    const response = await client.query<IConnectionListNameSearchResponse>({
      query: RETURN_ALL_CONNECTIONS_HOSTNAME_AND_CONTAINERID_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH(
        true,
        value.trim(),
        addressSpaceName,
        namespace
      ),
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    if (
      response &&
      response.data &&
      response.data.connections &&
      response.data.connections.connections &&
      response.data.connections.connections.length > 0
    ) {
      const obtainedList = response.data.connections.connections.map(
        (connection: any) => {
          return connection.spec.hostname;
        }
      );
      //get list of unique records to display in the select dropdown based on total records and 100 fetched objects
      const filteredHostnameOptions = getSelectOptionList(
        obtainedList,
        response.data.connections.total
      );
      if (filteredHostnameOptions.length > 0) return filteredHostnameOptions;
    }
  };

  const onContainerSelect = (e: any, selection: SelectOptionObject) => {
    setContainerSelected(selection.toString());
  };
  const onContainerClear = () => {
    setContainerSelected(undefined);
    setContainerInput(undefined);
  };

  const onChangeContainerInput = async (value: string) => {
    const response = await client.query<IConnectionListNameSearchResponse>({
      query: RETURN_ALL_CONNECTIONS_HOSTNAME_AND_CONTAINERID_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH(
        false,
        value.trim(),
        addressSpaceName,
        namespace
      ),
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    if (
      response &&
      response.data &&
      response.data.connections &&
      response.data.connections.connections &&
      response.data.connections.connections.length > 0
    ) {
      const obtainedList = response.data.connections.connections.map(
        (connection: any) => {
          return connection.spec.containerId;
        }
      );
      //get list of unique records to display in the select dropdown based on total records and 100 fetched objects
      const filteredContainerOptions = getSelectOptionList(
        obtainedList,
        response.data.connections.total
      );
      if (filteredContainerOptions.length > 0) return filteredContainerOptions;
    }
  };

  const onSearch = () => {
    if (filterSelected) {
      if (filterSelected.toLowerCase() === "hostname") {
        if (
          hostnameSelected &&
          hostnameSelected.trim() !== "" &&
          selectedHostnames
        )
          if (
            selectedHostnames
              .map(filter => filter.value)
              .indexOf(hostnameSelected) < 0
          )
            setSelectedHostnames([
              ...selectedHostnames,
              { value: hostnameSelected.trim(), isExact: true }
            ]);
        if (!hostnameSelected && hostnameInput && hostnameInput.trim() !== "")
          if (
            selectedHostnames
              .map(filter => filter.value)
              .indexOf(hostnameInput.trim()) < 0
          )
            setSelectedHostnames([
              ...selectedHostnames,
              { value: hostnameInput.trim(), isExact: false }
            ]);
        setHostnameSelected(undefined);
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
    setHostnameInput(undefined);
    setHostnameSelected(undefined);
    setContainerInput(undefined);
    setContainerSelected(undefined);
  };
  const onDelete = (
    category: string | DataToolbarChipGroup,
    chip: string | DataToolbarChip
  ) => {
    let index;
    switch (category && category.toString().toLocaleLowerCase()) {
      case "hostname":
        if (selectedHostnames && chip) {
          index = selectedHostnames
            .map(filter => filter.value)
            .indexOf(chip.toString());
          if (index >= 0) selectedHostnames.splice(index, 1);
          setSelectedHostnames([...selectedHostnames]);
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
    }
  };

  return (
    <ConnectionsToolbar
      totalRecords={totalRecords}
      filterSelected={filterSelected}
      hostnameSelected={hostnameSelected}
      hostnameInput={hostnameInput}
      containerSelected={containerSelected}
      containerInput={containerInput}
      selectedHostnames={selectedHostnames}
      selectedContainers={selectedContainers}
      onFilterSelect={onFilterSelect}
      onHostnameSelect={onHostnameSelect}
      onHostnameClear={onHostnameClear}
      onContainerSelect={onContainerSelect}
      onContainerClear={onContainerClear}
      onSearch={onSearch}
      onDelete={onDelete}
      sortValue={sortValue}
      setSortValue={setSortValue}
      onClearAllFilters={onClearAllFilters}
      onChangeHostNameInput={onChangeHostNameInput}
      setHostContainerInput={setContainerInput}
      onChangeContainerInput={onChangeContainerInput}
      setHostNameInput={setHostnameInput}
    />
  );
};
