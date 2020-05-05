/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import { useA11yRouteChange, useDocumentTitle, Loading } from "use-patternfly";
import {
  IRowData,
  IExtraData,
  ISortBy,
  SortByDirection
} from "@patternfly/react-table";
import {
  DELETE_ADDRESS_SPACE,
  RETURN_ALL_ADDRESS_SPACES,
  DOWNLOAD_CERTIFICATE
} from "graphql-module/queries";
import { IAddressSpacesResponse } from "schema/ResponseTypes";
import { FetchPolicy, POLL_INTERVAL } from "constant";
import { IObjectMeta_v1_Input } from "modules/address-space/AddressSpaceDetailPage";
import { useMutationQuery } from "hooks";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import { compareObject } from "utils";
import {
  getTableCells,
  getActionResolver,
  getTableColumns
} from "modules/address-space/utils";
import {
  AddressSpaceList,
  IAddressSpace,
  EmptyAddressSpace
} from "modules/address-space/components";

export interface IAddressSpaceListContainerProps {
  page: number;
  perPage: number;
  setTotalAddressSpaces: (value: number) => void;
  filterNames: string[];
  filterNamespaces: string[];
  filterType: string | null;
  filterStatus: string | null;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
  onSelectAddressSpace: (data: IAddressSpace, isSelected: boolean) => void;
  onSelectAllAddressSpace: (
    dataList: IAddressSpace[],
    isSelected: boolean
  ) => void;
  selectedAddressSpaces: Array<IAddressSpace>;
}

export const AddressSpaceListContainer: React.FC<IAddressSpaceListContainerProps> = ({
  page,
  perPage,
  setTotalAddressSpaces,
  filterNames,
  filterNamespaces,
  filterType,
  filterStatus,
  sortValue,
  setSortValue,
  onSelectAddressSpace,
  onSelectAllAddressSpace,
  selectedAddressSpaces
}) => {
  useDocumentTitle("Addressspace List");
  useA11yRouteChange();
  const client = useApolloClient();
  const { dispatch } = useStoreContext();
  const [sortBy, setSortBy] = useState<ISortBy>();
  const refetchQueries: string[] = ["all_address_spaces"];
  const [setDeleteAddressSpaceQueryVariables] = useMutationQuery(
    DELETE_ADDRESS_SPACE,
    refetchQueries
  );

  const { loading, data } = useQuery<IAddressSpacesResponse>(
    RETURN_ALL_ADDRESS_SPACES(
      page,
      perPage,
      filterNames,
      filterNamespaces,
      filterType,
      filterStatus,
      sortBy
    ),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  if (loading) {
    return <Loading />;
  }

  const { addressSpaces } = data || {
    addressSpaces: { total: 0, addressSpaces: [] }
  };

  setTotalAddressSpaces(addressSpaces && addressSpaces.total);

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
  const onDownloadCertificate = async (data: IObjectMeta_v1_Input) => {
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

  const addressSpacesList: IAddressSpace[] =
    addressSpaces &&
    addressSpaces.addressSpaces.map(addSpace => ({
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
          compareObject(
            { name, nameSpace: addSpace.metadata.namespace },
            { name: addSpace.metadata.name, nameSpace }
          )
        ).length === 1
    }));

  const onSort = (_event: any, index: number, direction: SortByDirection) => {
    setSortBy({ index: index, direction: direction });
    setSortValue({ index: index, direction: direction });
  };

  const tableRows =
    addressSpacesList && addressSpacesList.map(row => getTableCells(row));

  const onSelect = async (
    event: React.MouseEvent,
    isSelected: boolean,
    rowIndex: number,
    rowData: IRowData,
    extraData: IExtraData
  ) => {
    let rows;
    if (rowIndex === -1) {
      rows = tableRows.map(a => {
        const data = a;
        data.selected = isSelected;
        return data;
      });
      onSelectAllAddressSpace(
        rows.map(row => row.originalData),
        isSelected
      );
    } else {
      rows = [...tableRows];
      rows[rowIndex].selected = isSelected;
      onSelectAddressSpace(rows[rowIndex].originalData, isSelected);
    }
  };

  const actionResolver = (rowData: IRowData) => {
    return getActionResolver(
      rowData,
      onChangeEdit,
      onChangeDelete,
      onDownloadCertificate
    );
  };

  return (
    <>
      <AddressSpaceList
        onSelect={onSelect}
        onSort={onSort}
        rows={tableRows}
        cells={getTableColumns}
        sortBy={sortBy}
        actionResolver={actionResolver}
      />
      {(addressSpaces && addressSpaces.total) > 0 ? "" : <EmptyAddressSpace />}
    </>
  );
};
