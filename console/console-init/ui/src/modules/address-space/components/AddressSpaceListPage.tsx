/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { useA11yRouteChange, useDocumentTitle, Loading } from "use-patternfly";
import {
  AddressSpaceList,
  IAddressSpace
} from "modules/address-space/components/AddressSpaceList";
import { EmptyAddressSpace } from "modules/address-space/components/EmptyAddressSpace";
import {
  DELETE_ADDRESS_SPACE,
  RETURN_ALL_ADDRESS_SPACES,
  DOWNLOAD_CERTIFICATE
} from "graphql-module/queries";
import { IAddressSpacesResponse } from "types/ResponseTypes";
import { ISortBy } from "@patternfly/react-table";
import { compareTwoAddress } from "pages/AddressSpaceDetail/AddressList/AddressListPage";
import { FetchPolicy, POLL_INTERVAL } from "constants/constants";
import { IObjectMeta_v1_Input } from "pages/AddressSpaceDetail/AddressSpaceDetailPage";
import { useMutationQuery } from "hooks";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";

interface AddressSpaceListPageProps {
  page: number;
  perPage: number;
  totalAddressSpaces: number;
  setTotalAddressSpaces: (value: number) => void;
  filter_Names: string[];
  filter_NameSpace: string[];
  filter_Type: string | null;
  onCreationRefetch?: boolean;
  setOnCreationRefetch: (value: boolean) => void;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  isCreateWizardOpen: boolean;
  setIsCreateWizardOpen: (value: boolean) => void;
  onSelectAddressSpace: (data: IAddressSpace, isSelected: boolean) => void;
  onSelectAllAddressSpace: (
    dataList: IAddressSpace[],
    isSelected: boolean
  ) => void;
  selectedAddressSpaces: Array<IAddressSpace>;
}
export const AddressSpaceListPage: React.FunctionComponent<AddressSpaceListPageProps> = ({
  page,
  perPage,
  totalAddressSpaces,
  setTotalAddressSpaces,
  filter_Names,
  filter_NameSpace,
  filter_Type,
  onCreationRefetch,
  setOnCreationRefetch,
  sortValue,
  setSortValue,
  onSelectAddressSpace,
  onSelectAllAddressSpace,
  selectedAddressSpaces
}) => {
  useDocumentTitle("Addressspace List");
  useA11yRouteChange();
  const client = useApolloClient();
  const [sortBy, setSortBy] = React.useState<ISortBy>();
  const { dispatch } = useStoreContext();
  const refetchQueries: any[] = ["all_address_spaces"];
  const [setDeleteAddressSpaceQueryVariables] = useMutationQuery(
    DELETE_ADDRESS_SPACE,
    refetchQueries
  );

  const { loading, data, refetch } = useQuery<IAddressSpacesResponse>(
    RETURN_ALL_ADDRESS_SPACES(
      page,
      perPage,
      filter_Names,
      filter_NameSpace,
      filter_Type,
      sortBy
    ),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  if (loading) {
    return <Loading />;
  }

  if (onCreationRefetch) {
    refetch();
    setOnCreationRefetch(false);
  }

  const { addressSpaces } = data || {
    addressSpaces: { total: 0, addressSpaces: [] }
  };
  setTotalAddressSpaces(addressSpaces.total);

  if (sortValue && sortBy !== sortValue) {
    setSortBy(sortValue);
  }

  const onChangeEdit = (addressSpace: IAddressSpace) => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.EDIT_ADDRESS_SPACE,
      modalProps: {
        addressSpace
      }
    });
  };

  const onDeleteAddressSpace = (addressSpace: IAddressSpace) => {
    if (addressSpace) {
      const variables = {
        a: {
          name: addressSpace.name,
          namespace: addressSpace.nameSpace
        }
      };
      setDeleteAddressSpaceQueryVariables(variables);
    }
  };

  const onChangeDelete = (addressSpace: IAddressSpace) => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_ADDRESS_SPACE,
      modalProps: {
        selectedItems: [addressSpace.name],
        data: addressSpace,
        onConfirm: onDeleteAddressSpace,
        option: "Delete",
        detail: `Are you sure you want to delete this addressspace: ${addressSpace.name} ?`,
        header: "Delete this Address Space ?"
      }
    });
  };

  //Download the certificate function
  const downloadCertificate = async (data: IObjectMeta_v1_Input) => {
    const dataToDownload = await client.query({
      query: DOWNLOAD_CERTIFICATE,
      variables: {
        as: {
          name: data.name,
          namespace: data.namespace
        }
      },
      fetchPolicy: FetchPolicy.NETWORK_ONLY
    });
    if (dataToDownload.errors) {
      console.log("Error while download", dataToDownload.errors);
    }
    const url = window.URL.createObjectURL(
      new Blob([dataToDownload.data.messagingCertificateChain])
    );
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", `${data.name}.crt`);
    document.body.appendChild(link);
    link.click();
    if (link.parentNode) link.parentNode.removeChild(link);
  };

  const addressSpacesList: IAddressSpace[] = addressSpaces.addressSpaces.map(
    addSpace => ({
      name: addSpace.metadata.name,
      nameSpace: addSpace.metadata.namespace,
      creationTimestamp: addSpace.metadata.creationTimestamp,
      type: addSpace.spec.type,
      planValue: addSpace.spec.plan.metadata.name,
      displayName: addSpace.spec.plan.spec.displayName,
      isReady: addSpace.status && addSpace.status.isReady,
      phase:
        addSpace.status && addSpace.status.phase ? addSpace.status.phase : "",
      messages:
        addSpace.status && addSpace.status.messages
          ? addSpace.status.messages
          : [],
      authenticationService:
        addSpace.spec &&
        addSpace.spec.authenticationService &&
        addSpace.spec.authenticationService.name,
      selected:
        selectedAddressSpaces.filter(({ name, nameSpace }) =>
          compareTwoAddress(
            name,
            addSpace.metadata.name,
            nameSpace,
            addSpace.metadata.namespace
          )
        ).length === 1
    })
  );

  const onSort = (_event: any, index: any, direction: any) => {
    setSortBy({ index: index, direction: direction });
    setSortValue({ index: index, direction: direction });
  };

  return (
    <>
      {totalAddressSpaces > 0 ? (
        <AddressSpaceList
          rows={addressSpacesList}
          onSelectAddressSpace={onSelectAddressSpace}
          onSelectAllAddressSpace={onSelectAllAddressSpace}
          onEdit={onChangeEdit}
          onDelete={onChangeDelete}
          onSort={onSort}
          sortBy={sortBy}
          onDownload={downloadCertificate}
        />
      ) : (
        <EmptyAddressSpace />
      )}
    </>
  );
};
