/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useParams } from "react-router";
import {
  DataToolbar,
  DataToolbarItem,
  DataToolbarContent
} from "@patternfly/react-core/dist/js/experimental";
import { ISortBy } from "@patternfly/react-table";
import { useApolloClient } from "@apollo/react-hooks";
import { CreateAddress, IFilterValue, AddressListKebab } from "modules/address";
import { RETURN_ADDRESS_SPACE_DETAIL } from "graphql-module/queries";
import { IAddressSpacesResponse } from "types/ResponseTypes";
import { AddressListFilter } from "modules/address/containers/AddressFilter";
import { useWindowDimensions, SortForMobileView } from "components";
import { FetchPolicy } from "constants/constants";
import { Loading } from "use-patternfly";

export interface AddressListFilterProps {
  filterValue: string | null;
  setFilterValue: (value: string | null) => void;
  filterNames: Array<IFilterValue>;
  setFilterNames: (value: Array<IFilterValue>) => void;
  typeValue: string | null;
  setTypeValue: (value: string | null) => void;
  statusValue: string | null;
  setStatusValue: (value: string | null) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  setOnCreationRefetch?: (value: boolean) => void;
  totalAddresses: number;
  isCreateWizardOpen: boolean;
  setIsCreateWizardOpen: (value: boolean) => void;
  onDeleteAllAddress: () => void;
  onPurgeAllAddress: () => void;
  isDeleteAllDisabled: boolean;
  isPurgeAllDisabled: boolean;
}

export const AddressListToolbar: React.FunctionComponent<AddressListFilterProps> = ({
  filterValue,
  setFilterValue,
  filterNames,
  setFilterNames,
  typeValue,
  setTypeValue,
  statusValue,
  setStatusValue,
  sortValue,
  setSortValue,
  setOnCreationRefetch,
  totalAddresses,
  isCreateWizardOpen,
  setIsCreateWizardOpen,
  onDeleteAllAddress,
  onPurgeAllAddress,
  isDeleteAllDisabled,
  isPurgeAllDisabled
}) => {
  const { name, namespace, type } = useParams();
  const [addressSpacePlan, setAddressSpacePlan] = useState<string | null>(null);
  const client = useApolloClient();
  const { width } = useWindowDimensions();

  const onDeleteAll = () => {
    setFilterValue("Address");
    setTypeValue(null);
    setStatusValue(null);
    setFilterNames([]);
  };
  const sortMenuItems = [
    { key: "name", value: "Address", index: 1 },
    { key: "creationTimestamp", value: "Time Created", index: 4 },
    { key: "messageIn", value: "Message In", index: 5 },
    { key: "messageOut", value: "Message Out", index: 6 },
    { key: "storedMessage", value: "Stored Messages", index: 7 },
    { key: "senders", value: "Senders", index: 8 },
    { key: "receivers", value: "Receivers", index: 9 }
  ];

  const createAddressOnClick = async () => {
    if (name && namespace) {
      const { data, loading } = await client.query<IAddressSpacesResponse>({
        query: RETURN_ADDRESS_SPACE_DETAIL(name, namespace),
        fetchPolicy: FetchPolicy.NETWORK_ONLY
      });
      if (loading) {
        return <Loading />;
      }
      if (
        data &&
        data.addressSpaces &&
        data.addressSpaces.addressSpaces.length > 0
      ) {
        const plan =
          data.addressSpaces.addressSpaces[0].spec.plan.metadata.name;
        if (plan) {
          setAddressSpacePlan(plan);
        }
      }
    }
    setIsCreateWizardOpen(!isCreateWizardOpen);
  };

  const toolbarItems = (
    <>
      <AddressListFilter
        filterValue={filterValue}
        setFilterValue={setFilterValue}
        filterNames={filterNames}
        setFilterNames={setFilterNames}
        typeValue={typeValue}
        setTypeValue={setTypeValue}
        statusValue={statusValue}
        setStatusValue={setStatusValue}
        totalAddresses={totalAddresses}
        addressspaceName={name}
        namespace={namespace}
      />
      {width < 769 && (
        <SortForMobileView
          sortMenu={sortMenuItems}
          sortValue={sortValue}
          setSortValue={setSortValue}
        />
      )}
      <DataToolbarItem>
        {isCreateWizardOpen && (
          <CreateAddress
            name={name || ""}
            namespace={namespace || ""}
            addressSpace={name || ""}
            addressSpacePlan={addressSpacePlan}
            addressSpaceType={type}
            isCreateWizardOpen={isCreateWizardOpen}
            setIsCreateWizardOpen={setIsCreateWizardOpen}
            setOnCreationRefetch={setOnCreationRefetch}
          />
        )}
      </DataToolbarItem>
      <DataToolbarItem>
        <AddressListKebab
          createAddressOnClick={createAddressOnClick}
          onDeleteAllAddress={onDeleteAllAddress}
          onPurgeAllAddress={onPurgeAllAddress}
          isDeleteAllDisabled={isDeleteAllDisabled}
          isPurgeAllDisabled={isPurgeAllDisabled}
        />
      </DataToolbarItem>
    </>
  );
  return (
    <DataToolbar
      id="data-toolbar-with-filter"
      className="pf-m-toggle-group-container"
      collapseListedFiltersBreakpoint="xl"
      clearAllFilters={onDeleteAll}
    >
      <DataToolbarContent>{toolbarItems}</DataToolbarContent>
    </DataToolbar>
  );
};
