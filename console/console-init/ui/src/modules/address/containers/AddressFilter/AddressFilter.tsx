/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { DataToolbarChip } from "@patternfly/react-core/dist/js/experimental";
import { useApolloClient } from "@apollo/react-hooks";
import { IAddressListNameSearchResponse } from "types/ResponseTypes";
import { RETURN_ALL_ADDRESS_NAMES_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH } from "graphql-module/queries";
import { TYPEAHEAD_REQUIRED_LENGTH, FetchPolicy } from "constants/constants";
import { getSelectOptionList, ISelectOption } from "utils";
import { AddressToolbarToggleGroup } from "modules/address/components";

interface IAddressListFilterProps {
  filterValue: string | null;
  setFilterValue: (value: string | null) => void;
  filterNames: Array<{ value: string; isExact: boolean }>;
  setFilterNames: (value: Array<{ value: string; isExact: boolean }>) => void;
  typeValue: string | null;
  setTypeValue: (value: string | null) => void;
  statusValue: string | null;
  setStatusValue: (value: string | null) => void;
  totalAddresses: number;
  addressspaceName?: string;
  namespace?: string;
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
  const [filterIsExpanded, setFilterIsExpanded] = useState(false);
  const [typeIsExpanded, setTypeIsExpanded] = useState(false);
  const [statusIsExpanded, setStatusIsExpanded] = useState(false);
  const [nameSelected, setNameSelected] = useState<string>();
  const [nameOptions, setNameOptions] = useState<Array<ISelectOption>>();
  const [nameInput, setNameInput] = useState<string>("");

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

  const onDelete = (
    type: string | DataToolbarChip,
    id: string | DataToolbarChip
  ) => {
    switch (type) {
      case "Address":
        let index;
        if (filterNames && id) {
          index = filterNames
            .map(filter => filter.value)
            .indexOf(id.toString());
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
  return (
    <AddressToolbarToggleGroup
      totalAddresses={totalAddresses}
      onFilterSelect={onFilterSelect}
      filterIsExpanded={filterIsExpanded}
      setFilterIsExpanded={setFilterIsExpanded}
      statusIsExpanded={statusIsExpanded}
      setStatusIsExpanded={setStatusIsExpanded}
      typeIsExpanded={typeIsExpanded}
      setTypeIsExpanded={setTypeIsExpanded}
      filterValue={filterValue}
      filterNames={filterNames}
      onDelete={onDelete}
      onClickSearchIcon={onClickSearchIcon}
      nameOptions={nameOptions}
      nameSelected={nameSelected}
      setNameSelected={setNameSelected}
      nameInput={nameInput}
      setNameInput={setNameInput}
      onChangeNameData={onChangeNameData}
      typeValue={typeValue}
      statusValue={statusValue}
      onTypeSelect={onTypeSelect}
      onStatusSelect={onStatusSelect}
    />
  );
};
