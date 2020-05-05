/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useLocation } from "react-router";
import { useDocumentTitle, useA11yRouteChange } from "use-patternfly";
import {
  PageSection,
  PageSectionVariants,
  Grid,
  GridItem
} from "@patternfly/react-core";
import { Divider } from "@patternfly/react-core/dist/js/experimental";
import { ISortBy } from "@patternfly/react-table";
import { useMutation } from "@apollo/react-hooks";
import { IAddressSpace } from "./components";
import {
  AddressSpaceListContainer,
  MessagingToolbarContainer
} from "./containers";
import { DELETE_ADDRESS_SPACE } from "graphql-module/queries";
import { compareObject } from "utils";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import { getHeaderForDeleteDialog, getDetailForDeleteDialog } from "./utils";
import { TablePagination } from "components";

export default function AddressSpacePage() {
  const { dispatch } = useStoreContext();
  useDocumentTitle("Address Space List");
  useA11yRouteChange();
  let deleteAddressSpaceErrors: any = [];

  const [filterNames, setFilterNames] = useState<string[]>([]);
  const [filterNamespaces, setFilterNamespaces] = useState<string[]>([]);
  const [filterType, setFilterType] = useState<string | null>(null);
  const [filterStatus, setFilterStatus] = useState<string | null>(null);
  const [totalAddressSpaces, setTotalAddressSpaces] = useState<number>(0);
  const [sortDropDownValue, setSortDropdownValue] = useState<ISortBy>();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [selectedAddressSpaces, setSelectedAddressSpaces] = useState<
    IAddressSpace[]
  >([]);

  const refetchQueries: string[] = ["all_address_spaces"];
  const [setDeleteAddressSpaceQueryVariables] = useMutation(
    DELETE_ADDRESS_SPACE,
    {
      refetchQueries,
      awaitRefetchQueries: true
    }
  );

  const deleteAddressSpace = async (
    addressSpace: IAddressSpace,
    index: number
  ) => {
    try {
      const variables = {
        a: {
          name: addressSpace.name,
          namespace: addressSpace.nameSpace
        }
      };
      await setDeleteAddressSpaceQueryVariables({ variables });
    } catch (error) {
      deleteAddressSpaceErrors.push(error);
    }
    /**
     * dispatch action to set server errors after completion all queries
     */
    if (
      selectedAddressSpaces &&
      selectedAddressSpaces.length === index + 1 &&
      deleteAddressSpaceErrors.length > 0
    ) {
      dispatch({
        type: types.SET_SERVER_ERROR,
        payload: { errors: deleteAddressSpaceErrors }
      });
    }
  };

  const onDeleteAll = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_ADDRESS_SPACE,
      modalProps: {
        onConfirm: onConfirmDeleteAll,
        selectedItems: selectedAddressSpaces.map(as => as.name),
        option: "Delete",
        detail: getDetailForDeleteDialog(selectedAddressSpaces),
        header: getHeaderForDeleteDialog(selectedAddressSpaces)
      }
    });
  };

  const onConfirmDeleteAll = async () => {
    if (selectedAddressSpaces && selectedAddressSpaces.length > 0) {
      const data = selectedAddressSpaces;
      await Promise.all(
        data.map((addressSpace, index) =>
          deleteAddressSpace(addressSpace, index)
        )
      );
      setSelectedAddressSpaces([]);
    }
  };

  const onSelectAddressSpace = (data: IAddressSpace, isSelected: boolean) => {
    if (isSelected === true && selectedAddressSpaces.indexOf(data) === -1) {
      setSelectedAddressSpaces(prevState => [...prevState, data]);
    } else if (isSelected === false) {
      setSelectedAddressSpaces(prevState =>
        prevState.filter(
          addressSpace =>
            !compareObject(
              {
                name: addressSpace.name,
                nameSpace: addressSpace.nameSpace
              },
              {
                name: data.name,
                nameSpace: data.nameSpace
              }
            )
        )
      );
    }
  };

  const onSelectAllAddressSpace = (
    dataList: IAddressSpace[],
    isSelected: boolean
  ) => {
    if (isSelected === true) {
      setSelectedAddressSpaces(dataList);
    } else if (isSelected === false) {
      setSelectedAddressSpaces([]);
    }
  };

  const isDeleteAllOptionDisabled = () => {
    if (selectedAddressSpaces && selectedAddressSpaces.length > 0) {
      return false;
    }
    return true;
  };

  const renderPagination = () => {
    return (
      <TablePagination
        itemCount={totalAddressSpaces}
        variant={"top"}
        page={page}
        perPage={perPage}
      />
    );
  };

  return (
    <PageSection variant={PageSectionVariants.light}>
      <Grid>
        <GridItem span={7}>
          <MessagingToolbarContainer
            selectedNames={filterNames}
            setSelectedNames={setFilterNames}
            selectedNamespaces={filterNamespaces}
            setSelectedNamespaces={setFilterNamespaces}
            typeSelected={filterType}
            setTypeSelected={setFilterType}
            statusSelected={filterStatus}
            setStatusSelected={setFilterStatus}
            totalAddressSpaces={totalAddressSpaces}
            sortValue={sortDropDownValue}
            setSortValue={setSortDropdownValue}
            onDeleteAll={onDeleteAll}
            isDeleteAllDisabled={isDeleteAllOptionDisabled()}
          />
        </GridItem>
        <GridItem span={5}>{renderPagination()}</GridItem>
      </Grid>
      <Divider />
      <AddressSpaceListContainer
        page={page}
        perPage={perPage}
        setTotalAddressSpaces={setTotalAddressSpaces}
        filterNames={filterNames}
        filterNamespaces={filterNamespaces}
        filterType={filterType}
        filterStatus={filterStatus}
        sortValue={sortDropDownValue}
        setSortValue={setSortDropdownValue}
        selectedAddressSpaces={selectedAddressSpaces}
        onSelectAddressSpace={onSelectAddressSpace}
        onSelectAllAddressSpace={onSelectAllAddressSpace}
      />
      {renderPagination()}
    </PageSection>
  );
}
