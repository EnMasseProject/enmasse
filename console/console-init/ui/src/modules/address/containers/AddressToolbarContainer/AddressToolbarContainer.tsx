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
  RETURN_ALL_CONTAINER_IDS_OF_ADDRESS_LINKS_FOR_TYPEAHEAD_SEARCH,
  RETURN_ALL_ADDRESS_NAMES_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH,
  RETURN_ADDRESS_SPACE_DETAIL
} from "graphql-module/queries";
import {
  ISearchAddressLinkNameResponse,
  ISearchAddressLinkContainerResponse,
  IAddressListNameSearchResponse,
  IAddressSpacesResponse
} from "schema/ResponseTypes";
import { AddressLinksToolbar } from "modules/address-detail/components";
import { AddressToolbar } from "modules/address/components";
import { useParams } from "@reach/router";

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
  addressspaceType: string;
  onDeleteAllAddress: () => void;
  onPurgeAllAddress: () => void;
  isDeleteAllDisabled: boolean;
  isPurgeAllDisabled: boolean;
  setOnCreationRefetch: (value: boolean) => void;
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
  isPurgeAllDisabled,
  setOnCreationRefetch
}) => {
  const client = useApolloClient();
  const [nameSelected, setNameSelected] = useState<string>();
  const [nameInput, setNameInput] = useState<string>();
  const [nameOptions, setNameOptions] = useState<any[]>();
  const [typeIsExpanded, setTypeIsExpanded] = useState<boolean>(false);
  const [statusIsExpanded, setStatusIsExpanded] = useState<boolean>(false);
  const [filterSelected, setFilterSelected] = useState<string>("Name");
  const [addressSpacePlan, setAddressSpacePlan] = useState();
  const [isCreateWizardOpen, setIsCreateWizardOpen] = useState<boolean>(false);

  const onClearAllFilters = () => {
    setFilterSelected("Name");
    setSelectedNames([]);
    setTypeSelected(null);
    setStatusSelected(null);
  };

  if (!nameOptions) {
    setNameOptions([initalSelectOption]);
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

  const onTypeToggle = () => {
    setTypeIsExpanded(!typeIsExpanded);
  };
  const onTypeSelect = (e: any, selection: SelectOptionObject) => {
    setTypeSelected(selection.toString());
    setTypeIsExpanded(false);
  };

  const onStatusToggle = () => {
    setStatusIsExpanded(!statusIsExpanded);
  };
  const onStatusSelect = (e: any, selection: SelectOptionObject) => {
    setStatusSelected(selection.toString());
    setStatusIsExpanded(false);
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
      case "type":
        setTypeSelected(null);
        break;
      case "status":
        setStatusSelected(null);
        break;
    }
  };
  const createAddressOnClick = async () => {
    setIsCreateWizardOpen(!isCreateWizardOpen);
    if (addressspaceName && namespace) {
      const addressSpace = await client.query<IAddressSpacesResponse>({
        query: RETURN_ADDRESS_SPACE_DETAIL(addressspaceName, namespace),
        fetchPolicy: FetchPolicy.NETWORK_ONLY
      });
      if (
        addressSpace.data &&
        addressSpace.data.addressSpaces &&
        addressSpace.data.addressSpaces.addressSpaces.length > 0
      ) {
        const plan =
          addressSpace.data.addressSpaces.addressSpaces[0].spec.plan.metadata
            .name;
        if (plan) {
          setAddressSpacePlan(plan);
        }
      }
    }
  };
  return (
    <AddressToolbar
      totalRecords={totalRecords}
      filterSelected={filterSelected}
      nameSelected={nameSelected}
      nameInput={nameInput}
      nameOptions={nameOptions}
      typeIsExpanded={typeIsExpanded}
      typeSelected={typeSelected}
      statusIsExpanded={statusIsExpanded}
      statusSelected={statusSelected}
      selectedNames={selectedNames}
      onFilterSelect={onFilterSelect}
      onNameSelect={onNameSelect}
      onNameClear={onNameClear}
      onNameFilter={onNameFilter}
      onTypeToggle={onTypeToggle}
      onTypeSelect={onTypeSelect}
      onStatusToggle={onStatusToggle}
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
      onClickCreateAddress={createAddressOnClick}
      setOnCreationRefetch={setOnCreationRefetch}
      isCreateWizardOpen={isCreateWizardOpen}
      setIsCreateWizardOpen={setIsCreateWizardOpen}
      namespace={namespace}
      addressspaceName={addressspaceName}
      addressspaceType={addressspaceType}
      addressspacePlan={addressSpacePlan}
    />
  );
};
