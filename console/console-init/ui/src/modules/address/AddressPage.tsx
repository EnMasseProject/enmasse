/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useParams, useLocation } from "react-router-dom";

import {
  PageSection,
  PageSectionVariants,
  GridItem,
  Grid
} from "@patternfly/react-core";
import { useDocumentTitle, useA11yRouteChange } from "use-patternfly";
import { Divider } from "@patternfly/react-core";
import { useQuery } from "@apollo/react-hooks";
// import { StyleSheet } from "@patternfly/react-styles";
import { ISortBy } from "@patternfly/react-table";
import { AddressListContainer } from "./containers";
import {
  CURRENT_ADDRESS_SPACE_PLAN,
  DELETE_ADDRESS,
  PURGE_ADDRESS
} from "graphql-module/queries";
import { IAddress } from "./components";
import { useStoreContext, types, MODAL_TYPES } from "context-state-reducer";
import {
  getFilteredAdressNames,
  getHeaderTextForPurgeAll,
  getDetailTextForPurgeAll,
  getHeaderTextForDeleteAll,
  getDetailTextForDeleteAll,
  getFilteredAddressesByType,
  IFilterValue
} from "modules/address/utils";
import { compareObject } from "utils";
import { TablePagination } from "components/TablePagination";
import { AddressTypes } from "constant";
import { AddressToolbarContainer } from "modules/address/containers";
import { useMutationQuery } from "hooks";

// export const GridStylesForTableHeader = StyleSheet.create({
//   filter_left_margin: {
//     marginLeft: 24
//   },
//   create_button_left_margin: {
//     marginLeft: 10
//   }
// });

export interface IAddressSpacePlanResponse {
  addressSpaces: {
    addressSpaces: Array<{
      spec: {
        plan: {
          metadata: {
            name: string;
          };
        };
      };
    }>;
  };
}

export default function AddressPage() {
  const { dispatch } = useStoreContext();
  useDocumentTitle("Address List");
  useA11yRouteChange();
  const { name, namespace, type } = useParams();
  const [filterNames, setFilterNames] = useState<Array<IFilterValue>>([]);
  const [typeValue, setTypeValue] = useState<string | null>(null);
  const [statusValue, setStatusValue] = useState<string | null>(null);
  const [totalAddresses, setTotalAddress] = useState<number>(0);
  const [addressSpacePlan, setAddressSpacePlan] = useState<string | null>(null);

  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;

  const [sortDropDownValue, setSortDropdownValue] = useState<ISortBy>();

  const [selectedAddresses, setSelectedAddresses] = useState<IAddress[]>([]);
  const { data } = useQuery<IAddressSpacePlanResponse>(
    CURRENT_ADDRESS_SPACE_PLAN(name, namespace)
  );

  const refetchQueries: string[] = ["all_addresses_for_addressspace_view"];
  const [setDeleteAdressQueryVariables] = useMutationQuery(
    DELETE_ADDRESS,
    refetchQueries
  );
  const [setPurgeAddressQueryVariables] = useMutationQuery(
    PURGE_ADDRESS,
    refetchQueries
  );

  const { addressSpaces } = data || {
    addressSpaces: { addressSpaces: [] }
  };

  if (!addressSpacePlan && addressSpaces.addressSpaces[0]) {
    let name =
      addressSpaces.addressSpaces[0] &&
      addressSpaces.addressSpaces[0].spec &&
      addressSpaces.addressSpaces[0].spec.plan &&
      addressSpaces.addressSpaces[0].spec.plan.metadata &&
      addressSpaces.addressSpaces[0].spec.plan.metadata.name;
    setAddressSpacePlan(name);
  }

  const onDeleteAll = () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.DELETE_ADDRESS,
      modalProps: {
        option: "Delete",
        header: getHeaderTextForDeleteAll(selectedAddresses),
        detail: getDetailTextForDeleteAll(selectedAddresses),
        onConfirm: onConfirmDeleteAll,
        selectedItems: selectedAddresses.map(as => as.name)
      }
    });
  };

  const onPurgeAll = async () => {
    dispatch({
      type: types.SHOW_MODAL,
      modalType: MODAL_TYPES.PURGE_ADDRESS,
      modalProps: {
        option: "Purge",
        header: getHeaderTextForPurgeAll(selectedAddresses),
        onConfirm: onConfirmPurgeAll,
        selectedItems: getFilteredAdressNames(selectedAddresses),
        detail: getDetailTextForPurgeAll(selectedAddresses)
      }
    });
  };

  const onConfirmDeleteAll = async () => {
    if (selectedAddresses && selectedAddresses.length > 0) {
      let queryVariables: Array<{ name: string; namespace: string }> = [];
      selectedAddresses.map((address: IAddress) =>
        queryVariables.push({
          name: address.name,
          namespace: address.namespace
        })
      );
      if (queryVariables.length > 0) {
        const queryVariable = {
          a: queryVariables
        };
        await setDeleteAdressQueryVariables(queryVariable);
      }
      setSelectedAddresses([]);
    }
  };

  const onConfirmPurgeAll = async () => {
    const filteredAddresses = getFilteredAddressesByType(selectedAddresses);
    if (filteredAddresses && filteredAddresses.length > 0) {
      let variables: any[] = [];
      filteredAddresses.map((address: IAddress) =>
        variables.push({
          name: address.name,
          namespace: address.namespace
        })
      );
      if (variables.length > 0) {
        const queryVariable = {
          addrs: variables
        };
        await setPurgeAddressQueryVariables(queryVariable);
      }
      setSelectedAddresses([]);
    }
  };

  const onSelectAddress = (data: IAddress, isSelected: boolean) => {
    if (isSelected === true && selectedAddresses.indexOf(data) === -1) {
      setSelectedAddresses(prevState => [...prevState, data]);
    } else if (isSelected === false) {
      setSelectedAddresses(prevState =>
        prevState.filter(
          address =>
            !compareObject(
              { name: address.name, namespace: address.namespace },
              { name: data.name, namespace: data.namespace }
            )
        )
      );
    }
  };

  const onSelectAllAddress = (dataList: IAddress[], isSelected: boolean) => {
    if (isSelected === true) {
      setSelectedAddresses(dataList);
    } else if (isSelected === false) {
      setSelectedAddresses([]);
    }
  };

  const isDeleteAllOptionDisabled = () => {
    if (selectedAddresses.length > 0) {
      return false;
    }
    return true;
  };

  const isPurgeAllOptionDisbled = () => {
    const filteredAddresses = selectedAddresses.filter(
      address =>
        address.type.toLowerCase() === AddressTypes.QUEUE ||
        address.type.toLowerCase() === AddressTypes.SUBSCRIPTION
    );

    if (filteredAddresses.length > 0) {
      return false;
    }
    return true;
  };

  const renderPagination = () => {
    return (
      <TablePagination
        itemCount={totalAddresses}
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
          <AddressToolbarContainer
            selectedNames={filterNames}
            setSelectedNames={setFilterNames}
            typeSelected={typeValue}
            setTypeSelected={setTypeValue}
            statusSelected={statusValue}
            setStatusSelected={setStatusValue}
            totalRecords={totalAddresses}
            sortValue={sortDropDownValue}
            setSortValue={setSortDropdownValue}
            namespace={namespace || ""}
            addressspaceName={name || ""}
            addressspaceType={type || ""}
            onDeleteAllAddress={onDeleteAll}
            onPurgeAllAddress={onPurgeAll}
            isDeleteAllDisabled={isDeleteAllOptionDisabled()}
            isPurgeAllDisabled={isPurgeAllOptionDisbled()}
          />
        </GridItem>
        <GridItem span={5}>{renderPagination()}</GridItem>
      </Grid>
      <Divider />
      <AddressListContainer
        name={name}
        namespace={namespace}
        addressSpacePlan={addressSpacePlan}
        filterNames={filterNames}
        typeValue={typeValue}
        statusValue={statusValue}
        setTotalAddress={setTotalAddress}
        page={page}
        perPage={perPage}
        sortValue={sortDropDownValue}
        setSortValue={setSortDropdownValue}
        selectedAddresses={selectedAddresses}
        onSelectAddress={onSelectAddress}
        onSelectAllAddress={onSelectAllAddress}
      />
      {renderPagination()}
    </PageSection>
  );
}
