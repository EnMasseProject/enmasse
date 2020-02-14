/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { useParams, useLocation, useHistory } from "react-router-dom";

import {
  PageSection,
  PageSectionVariants,
  Pagination,
  GridItem,
  Grid
} from "@patternfly/react-core";
import { useDocumentTitle, useA11yRouteChange } from "use-patternfly";
import { StyleSheet } from "@patternfly/react-styles";
import { AddressListFilterPage } from "./AddressListFilterPage";
import { AddressListPage, compareTwoAddress } from "./AddressListPage";
import { Divider } from "@patternfly/react-core/dist/js/experimental";
import { useQuery, useApolloClient } from "@apollo/react-hooks";
import {
  CURRENT_ADDRESS_SPACE_PLAN,
  DELETE_ADDRESS,
  PURGE_ADDRESS
} from "queries";
import { ISortBy } from "@patternfly/react-table";
import { IAddress } from "components/AddressSpace/Address/AddressList";
import { DialoguePrompt } from "components/common/DialoguePrompt";
import { ErrorBoundary } from "components/common/ErrorBoundary";

export const GridStylesForTableHeader = StyleSheet.create({
  filter_left_margin: {
    marginLeft: 24
  },
  create_button_left_margin: {
    marginLeft: 10
  }
});

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

export default function AddressesList() {
  useDocumentTitle("Address List");
  useA11yRouteChange();
  const { name, namespace, type } = useParams();
  const [filterValue, setFilterValue] = React.useState<string | null>(
    "Address"
  );
  const [filterNames, setFilterNames] = React.useState<any[]>([]);
  const [typeValue, setTypeValue] = React.useState<string | null>(null);
  const [statusValue, setStatusValue] = React.useState<string | null>(null);
  const [totalAddresses, setTotalAddress] = React.useState<number>(0);
  const [addressSpacePlan, setAddressSpacePlan] = React.useState<string | null>(
    null
  );
  const location = useLocation();
  const history = useHistory();
  const client = useApolloClient();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [sortDropDownValue, setSortDropdownValue] = React.useState<ISortBy>();
  const [isCreateWizardOpen, setIsCreateWizardOpen] = React.useState(false);
  const [onCreationRefetch, setOnCreationRefetch] = React.useState<boolean>(
    false
  );

  const [selectedAddresses, setSelectedAddresses] = React.useState<IAddress[]>(
    []
  );

  const [isDisplayDeleteDailogue, setIsDisplayDeleteDailogue] = React.useState<
    boolean
  >(false);
  const [isDeleteAllDisabled, setIsDeleteAllDisabled] = React.useState<boolean>(
    true
  );

  const [isDisplayPurgeDailogue, setIsDisplayPurgeDailogue] = React.useState<
    boolean
  >(false);
  const [isPurgeAllDisabled, setIsPurgeAllDisabled] = React.useState<boolean>(
    true
  );
  const { data } = useQuery<IAddressSpacePlanResponse>(
    CURRENT_ADDRESS_SPACE_PLAN(name, namespace)
  );

  const { addressSpaces } = data || {
    addressSpaces: { addressSpaces: [] }
  };

  if (!addressSpacePlan && addressSpaces.addressSpaces[0]) {
    setAddressSpacePlan(addressSpaces.addressSpaces[0].spec.plan.metadata.name);
  }

  const setSearchParam = React.useCallback(
    (name: string, value: string) => {
      searchParams.set(name, value.toString());
    },
    [searchParams]
  );

  const handlePageChange = React.useCallback(
    (_: any, newPage: number) => {
      setSearchParam("page", newPage.toString());
      history.push({
        search: searchParams.toString()
      });
    },
    [setSearchParam, history, searchParams]
  );

  const handlePerPageChange = React.useCallback(
    (_: any, newPerPage: number) => {
      setSearchParam("page", "1");
      setSearchParam("perPage", newPerPage.toString());
      history.push({
        search: searchParams.toString()
      });
    },
    [setSearchParam, history, searchParams]
  );

  const renderPagination = (page: number, perPage: number) => {
    return (
      <Pagination
        itemCount={totalAddresses}
        perPage={perPage}
        page={page}
        onSetPage={handlePageChange}
        variant="top"
        onPerPageSelect={handlePerPageChange}
      />
    );
  };

  let deleteErrorData = [],
    purgeErrorData = [];

  const deleteAddress = async (data: any) => {
    const deletedData = await client.mutate({
      mutation: DELETE_ADDRESS,
      variables: {
        a: {
          name: data.name,
          namespace: data.namespace
        }
      }
    });
    if (deletedData.errors) {
      deleteErrorData.push(deletedData);
    }
    if (deletedData.data) {
      return deletedData;
    }
  };

  const purgeAddress = async (data: any) => {
    const purgeData = await client.mutate({
      mutation: PURGE_ADDRESS,
      variables: {
        a: {
          name: data.name,
          namespace: data.namespace
        }
      }
    });
    if (purgeData.errors) {
      purgeErrorData.push(purgeData);
    }
    if (purgeData.data) {
      return purgeData;
    }
  };

  React.useEffect(() => {
    if (selectedAddresses.length === 0 && !isDeleteAllDisabled) {
      setIsDeleteAllDisabled(true);
    } else if (selectedAddresses.length > 0 && isDeleteAllDisabled) {
      setIsDeleteAllDisabled(false);
    }
  }, [selectedAddresses]);

  React.useEffect(() => {
    const filteredAddresses = selectedAddresses.filter(
      address =>
        address.type.toLowerCase() === "queue" ||
        address.type.toLowerCase() === "subscription"
    );
    if (filteredAddresses.length === 0 && !isPurgeAllDisabled) {
      setIsPurgeAllDisabled(true);
    } else if (filteredAddresses.length > 0 && isPurgeAllDisabled) {
      setIsPurgeAllDisabled(false);
    }
  }, [selectedAddresses]);

  const onDeleteAll = async () => {
    setIsDisplayDeleteDailogue(true);
  };

  const onPurgeAll = async () => {
    setIsDisplayPurgeDailogue(true);
  };

  const handleCancelDeleteSelected = () => {
    setIsDisplayDeleteDailogue(false);
  };
  const handleConfirmDeleteSelected = async () => {
    if (selectedAddresses && selectedAddresses.length > 0) {
      const data = selectedAddresses;
      await Promise.all(data.map(address => deleteAddress(address)));
      setSelectedAddresses([]);
    }
    setOnCreationRefetch(true);
    setIsDisplayDeleteDailogue(false);
  };

  const handleConfirmPurgeSelected = async () => {
    const filteredAddresses = selectedAddresses.filter(
      address =>
        address.type.toLowerCase() === "queue" ||
        address.type.toLowerCase() === "subscription"
    );
    if (filteredAddresses && filteredAddresses.length > 0) {
      const data = filteredAddresses;
      await Promise.all(data.map(address => purgeAddress(address)));
      setSelectedAddresses([]);
    }
    setOnCreationRefetch(true);
    setIsDisplayPurgeDailogue(false);
  };
  const handleCancelPurgeSelected = () => {
    setIsDisplayPurgeDailogue(false);
  };
  const onSelectAddress = (data: IAddress, isSelected: boolean) => {
    if (isSelected === true && selectedAddresses.indexOf(data) === -1) {
      setSelectedAddresses(prevState => [...prevState, data]);
    } else if (isSelected === false) {
      setSelectedAddresses(prevState =>
        prevState.filter(
          address =>
            !compareTwoAddress(
              address.name,
              data.name,
              address.namespace,
              data.namespace
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

  return (
    <PageSection variant={PageSectionVariants.light}>
      <Grid>
        <GridItem span={7}>
          <AddressListFilterPage
            filterValue={filterValue}
            setFilterValue={setFilterValue}
            filterNames={filterNames}
            setFilterNames={setFilterNames}
            typeValue={typeValue}
            setTypeValue={setTypeValue}
            statusValue={statusValue}
            setStatusValue={setStatusValue}
            totalAddresses={totalAddresses}
            sortValue={sortDropDownValue}
            setOnCreationRefetch={setOnCreationRefetch}
            setSortValue={setSortDropdownValue}
            isCreateWizardOpen={isCreateWizardOpen}
            setIsCreateWizardOpen={setIsCreateWizardOpen}
            onDeleteAllAddress={onDeleteAll}
            onPurgeAllAddress={onPurgeAll}
            isDeleteAllDisabled={isDeleteAllDisabled}
            isPurgeAllDisabled={isPurgeAllDisabled}
          />
        </GridItem>
        <GridItem span={5}>
          {totalAddresses > 0 && renderPagination(page, perPage)}
        </GridItem>
      </Grid>
      <Divider />
      <AddressListPage
        name={name}
        namespace={namespace}
        addressSpacePlan={addressSpacePlan}
        filterNames={filterNames}
        typeValue={typeValue}
        statusValue={statusValue}
        setTotalAddress={setTotalAddress}
        page={page}
        perPage={perPage}
        addressSpaceType={type}
        sortValue={sortDropDownValue}
        setSortValue={setSortDropdownValue}
        isWizardOpen={isCreateWizardOpen}
        setIsWizardOpen={setIsCreateWizardOpen}
        onCreationRefetch={onCreationRefetch}
        setOnCreationRefetch={setOnCreationRefetch}
        selectedAddresses={selectedAddresses}
        onSelectAddress={onSelectAddress}
        onSelectAllAddress={onSelectAllAddress}
      />

      {totalAddresses > 0 && renderPagination(page, perPage)}
      {isDisplayDeleteDailogue && selectedAddresses.length > 0 && (
        <DialoguePrompt
          option="Delete"
          detail={
            selectedAddresses.length > 1
              ? `Are you sure you want to delete all of these addresses: ${selectedAddresses.map(
                  as => " " + as.displayName
                )} ?`
              : `Are you sure you want to delete this address: ${selectedAddresses[0].displayName} ?`
          }
          names={selectedAddresses.map(as => as.name)}
          header={
            selectedAddresses.length > 1
              ? "Delete these Addresses ?"
              : "Delete this Address ?"
          }
          handleCancelDialogue={handleCancelDeleteSelected}
          handleConfirmDialogue={handleConfirmDeleteSelected}
        />
      )}
      {isDisplayPurgeDailogue &&
        selectedAddresses.filter(
          address =>
            address.type.toLowerCase() === "queue" ||
            address.type.toLowerCase() === "subscription"
        ).length > 0 && (
          <DialoguePrompt
            option="Purge"
            detail={
              selectedAddresses.filter(
                address =>
                  address.type.toLowerCase() === "queue" ||
                  address.type.toLowerCase() === "subscription"
              ).length > 1
                ? `Are you sure you want to purge all of these addresses: ${selectedAddresses
                    .filter(
                      address =>
                        address.type.toLowerCase() === "queue" ||
                        address.type.toLowerCase() === "subscription"
                    )
                    .map(address => " " + address.displayName)} ?`
                : `Are you sure you want to purge this address: ${selectedAddresses.filter(
                    address =>
                      address.type.toLowerCase() === "queue" ||
                      address.type.toLowerCase() === "subscription"
                  ) &&
                    selectedAddresses.filter(
                      address =>
                        address.type.toLowerCase() === "queue" ||
                        address.type.toLowerCase() === "subscription"
                    )[0].displayName} ?`
            }
            names={selectedAddresses
              .filter(
                address =>
                  address.type.toLowerCase() === "queue" ||
                  address.type.toLowerCase() === "subscription"
              )
              .map(address => address.name)}
            header={
              selectedAddresses.filter(
                address =>
                  address.type.toLowerCase() === "queue" ||
                  address.type.toLowerCase() === "subscription"
              ).length > 1
                ? "Purge these Addresses ?"
                : "Purge this Address ?"
            }
            handleCancelDialogue={handleCancelPurgeSelected}
            handleConfirmDialogue={handleConfirmPurgeSelected}
          />
        )}
    </PageSection>
  );
}
