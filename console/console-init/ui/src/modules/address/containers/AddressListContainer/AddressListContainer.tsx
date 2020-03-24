/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useQuery } from "@apollo/react-hooks";
import { Loading } from "use-patternfly";
import { ISortBy } from "@patternfly/react-table";
import { IAddressResponse } from "schema/ResponseTypes";
import {
  RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE,
  DELETE_ADDRESS,
  PURGE_ADDRESS
} from "graphql-module/queries";
import { AddressList, IAddress } from "modules/address/components";
import { EmptyAddress } from "modules/address/components";
import { FetchPolicy, POLL_INTERVAL } from "constant";
import { useMutationQuery } from "hooks";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import { compareObject, getFilteredValue } from "utils";

export interface IAddressListPageProps {
  name?: string;
  namespace?: string;
  filterNames?: any[];
  typeValue?: string | null;
  statusValue?: string | null;
  page: number;
  perPage: number;
  setTotalAddress: (total: number) => void;
  addressSpacePlan: string | null;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  isWizardOpen: boolean;
  setIsWizardOpen: (value: boolean) => void;
  onCreationRefetch?: boolean;
  setOnCreationRefetch: (value: boolean) => void;
  selectedAddresses: Array<IAddress>;
  onSelectAddress: (address: IAddress, isSelected: boolean) => void;
  onSelectAllAddress: (addresses: IAddress[], isSelected: boolean) => void;
}

export const AddressListContainer: React.FunctionComponent<IAddressListPageProps> = ({
  name,
  namespace,
  filterNames,
  typeValue,
  statusValue,
  setTotalAddress,
  page,
  perPage,
  addressSpacePlan,
  sortValue,
  setSortValue,
  isWizardOpen,
  setIsWizardOpen,
  onCreationRefetch,
  setOnCreationRefetch,
  selectedAddresses,
  onSelectAddress,
  onSelectAllAddress
}) => {
  const { dispatch } = useStoreContext();

  const [sortBy, setSortBy] = useState<ISortBy>();

  const refetchQueries: string[] = ["all_addresses_for_addressspace_view"];

  const [setDeleteAddressQueryVariablse] = useMutationQuery(
    DELETE_ADDRESS,
    refetchQueries
  );

  const [setPurgeAddressQueryVariables] = useMutationQuery(
    PURGE_ADDRESS,
    refetchQueries
  );

  if (sortValue && sortBy !== sortValue) {
    setSortBy(sortValue);
  }
  const { data, refetch, loading } = useQuery<IAddressResponse>(
    RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE(
      page,
      perPage,
      name,
      namespace,
      filterNames,
      typeValue,
      statusValue,
      sortBy
    ),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  if (onCreationRefetch) {
    refetch();
    setOnCreationRefetch(false);
  }

  if (loading) return <Loading />;

  const { addresses } = data || {
    addresses: { total: 0, addresses: [] }
  };
  setTotalAddress(addresses.total);

  const addressesList: IAddress[] = addresses.addresses.map(address => ({
    name: address.metadata.name,
    displayName: address.spec.address,
    namespace: address.metadata.namespace,
    type: address.spec.type,
    planLabel:
      address.spec.plan.spec.displayName || address.spec.plan.metadata.name,
    planValue: address.spec.plan.metadata.name,
    messageIn: getFilteredValue(address.metrics, "enmasse_messages_in"),
    messageOut: getFilteredValue(address.metrics, "enmasse_messages_out"),
    storedMessages: getFilteredValue(
      address.metrics,
      "enmasse_messages_stored"
    ),
    senders: getFilteredValue(address.metrics, "enmasse_senders"),
    receivers: getFilteredValue(address.metrics, "enmasse_receivers"),
    partitions:
      address.status && address.status.planStatus
        ? address.status.planStatus.partitions
        : null,
    isReady: address.status && address.status.isReady,
    creationTimestamp: address.metadata.creationTimestamp,
    status: address.status && address.status.phase ? address.status.phase : "",
    errorMessages:
      address.status && address.status.messages ? address.status.messages : [],
    selected:
      selectedAddresses.filter(({ name, namespace }) =>
        compareObject(
          { name, namespace },
          { name: address.metadata.name, namespace: address.metadata.namespace }
        )
      ).length === 1
  }));

  const onPurge = async (address: IAddress) => {
    if (address) {
      const variables = {
        a: {
          name: address.name,
          namespace: address.namespace
        }
      };
      setPurgeAddressQueryVariables(variables);
    }
  };

  const onChangePurge = (address: IAddress) => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.PURGE_ADDRESS,
      modalProps: {
        data: address,
        onConfirm: onPurge,
        selectedItems: [address.name],
        option: "Purge",
        detail: `Are you sure you want to purge this address: ${address.displayName} ?`,
        header: "Purge this Address  ?"
      }
    });
  };
  const onChangeEdit = (address: IAddress) => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.EDIT_ADDRESS,
      modalProps: {
        address,
        addressSpacePlan
      }
    });
  };

  const onDelete = async (address: IAddress) => {
    if (address) {
      const variables = {
        a: {
          name: address.name,
          namespace: address.namespace
        }
      };
      setDeleteAddressQueryVariablse(variables);
    }
  };

  const onChangeDelete = (address: IAddress) => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_ADDRESS,
      modalProps: {
        data: address,
        onConfirm: onDelete,
        selectedItems: [address.name],
        option: "Delete",
        detail: `Are you sure you want to delete this address: ${address.displayName} ?`,
        header: "Delete this Address  ?"
      }
    });
  };

  const onSort = (_event: any, index: any, direction: any) => {
    setSortBy({ index: index, direction: direction });
    setSortValue({ index: index, direction: direction });
  };

  return (
    <>
      <AddressList
        rowsData={addressesList ? addressesList : []}
        onEdit={onChangeEdit}
        onDelete={onChangeDelete}
        onPurge={onChangePurge}
        sortBy={sortBy}
        onSort={onSort}
        onSelectAddress={onSelectAddress}
        onSelectAllAddress={onSelectAllAddress}
      />
      {addresses.total > 0 ? (
        " "
      ) : (
        <EmptyAddress
          isWizardOpen={isWizardOpen}
          setIsWizardOpen={setIsWizardOpen}
        />
      )}
    </>
  );
};
