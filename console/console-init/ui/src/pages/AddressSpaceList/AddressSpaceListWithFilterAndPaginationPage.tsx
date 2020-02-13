/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { useDocumentTitle, useA11yRouteChange } from "use-patternfly";
import { useLocation, useHistory } from "react-router";
import {
  Pagination,
  PageSection,
  PageSectionVariants,
  Grid,
  GridItem
} from "@patternfly/react-core";
import { AddressSpaceListPage } from "./AddressSpaceListPage";
import { AddressSpaceListFilterPage } from "./AddressSpaceListFilterPage";
import { Divider } from "@patternfly/react-core/dist/js/experimental";
import { ISortBy } from "@patternfly/react-table";
import { DELETE_ADDRESS_SPACE } from "queries";
import { useApolloClient } from "@apollo/react-hooks";
import { IAddressSpace } from "components/AddressSpaceList/AddressSpaceList";
import { compareTwoAddress } from "pages/AddressSpaceDetail/AddressList/AddressListPage";
import { DialoguePrompt } from "components/common/DialoguePrompt";

export default function AddressSpaceListWithFilterAndPagination() {
  useDocumentTitle("Address Space List");
  useA11yRouteChange();
  const client = useApolloClient();
  const [filterValue, setFilterValue] = React.useState<string>("Name");
  const [filterNames, setFilterNames] = React.useState<string[]>([]);
  const [onCreationRefetch, setOnCreationRefetch] = React.useState<boolean>(
    false
  );
  const [filterNamespaces, setFilterNamespaces] = React.useState<string[]>([]);
  const [filterType, setFilterType] = React.useState<string | null>(null);
  const [totalAddressSpaces, setTotalAddressSpaces] = React.useState<number>(0);
  const [sortDropDownValue, setSortDropdownValue] = React.useState<ISortBy>();
  const [isCreateWizardOpen, setIsCreateWizardOpen] = React.useState(false);
  const location = useLocation();
  const history = useHistory();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [selectedAddressSpaces, setSelectedAddressSpaces] = React.useState<
    IAddressSpace[]
  >([]);
  const [isDisplayDeleteDailogue, setIsDisplayDeleteDailogue] = React.useState<
    boolean
  >(false);
  const [isDeleteAllDisabled, setIsDeleteAllDisabled] = React.useState<boolean>(
    true
  );
  const setSearchParam = React.useCallback(
    (name: string, value: string) => {
      searchParams.set(name, value.toString());
    },
    [searchParams]
  );
  React.useEffect(() => {
    if (selectedAddressSpaces.length === 0 && !isDeleteAllDisabled) {
      setIsDeleteAllDisabled(true);
    } else if (selectedAddressSpaces.length > 0 && isDeleteAllDisabled) {
      setIsDeleteAllDisabled(false);
    }
  }, [selectedAddressSpaces]);
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
        itemCount={totalAddressSpaces}
        perPage={perPage}
        page={page}
        onSetPage={handlePageChange}
        variant="top"
        onPerPageSelect={handlePerPageChange}
      />
    );
  };
  let errorData = [];
  const deleteAddressSpace = async (data: IAddressSpace) => {
    const deletedData = await client.mutate({
      mutation: DELETE_ADDRESS_SPACE,
      variables: {
        a: {
          name: data.name,
          namespace: data.nameSpace
        }
      }
    });
    if (deletedData.errors) {
      errorData.push(data);
    }
    if (deletedData.data) {
      return deletedData;
    }
  };
  const onDeleteAll = () => {
    setIsDisplayDeleteDailogue(true);
  };

  const handleCancelDelete = () => {
    setIsDisplayDeleteDailogue(false);
  };
  const handleConfirmDelete = async () => {
    if (selectedAddressSpaces && selectedAddressSpaces.length > 0) {
      const data = selectedAddressSpaces;
      await Promise.all(
        data.map(addressSpace => deleteAddressSpace(addressSpace))
      );
      setSelectedAddressSpaces([]);
    }
    setOnCreationRefetch(true);
    setIsDisplayDeleteDailogue(false);
  };
  const onSelectAddressSpace = (data: IAddressSpace, isSelected: boolean) => {
    if (isSelected === true && selectedAddressSpaces.indexOf(data) === -1) {
      setSelectedAddressSpaces(prevState => [...prevState, data]);
    } else if (isSelected === false) {
      setSelectedAddressSpaces(prevState =>
        prevState.filter(
          addressSpace =>
            !compareTwoAddress(
              addressSpace.name,
              data.name,
              addressSpace.nameSpace,
              data.nameSpace
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

  return (
    <PageSection variant={PageSectionVariants.light}>
      <Grid>
        <GridItem span={7}>
          <AddressSpaceListFilterPage
            filterValue={filterValue}
            setFilterValue={setFilterValue}
            filterNames={filterNames}
            setFilterNames={setFilterNames}
            filterNamespaces={filterNamespaces}
            setFilterNamespaces={setFilterNamespaces}
            filterType={filterType}
            setFilterType={setFilterType}
            totalAddressSpaces={totalAddressSpaces}
            setOnCreationRefetch={setOnCreationRefetch}
            sortValue={sortDropDownValue}
            setSortValue={setSortDropdownValue}
            isCreateWizardOpen={isCreateWizardOpen}
            setIsCreateWizardOpen={setIsCreateWizardOpen}
            onDeleteAll={onDeleteAll}
            isDeleteAllDisabled={isDeleteAllDisabled}
          />
        </GridItem>
        <GridItem span={5}>
          {totalAddressSpaces > 0 && renderPagination(page, perPage)}
        </GridItem>
      </Grid>
      <Divider />
      <AddressSpaceListPage
        page={page}
        perPage={perPage}
        totalAddressSpaces={totalAddressSpaces}
        setTotalAddressSpaces={setTotalAddressSpaces}
        filter_Names={filterNames}
        filter_NameSpace={filterNamespaces}
        filter_Type={filterType}
        onCreationRefetch={onCreationRefetch}
        setOnCreationRefetch={setOnCreationRefetch}
        sortValue={sortDropDownValue}
        setSortValue={setSortDropdownValue}
        isCreateWizardOpen={isCreateWizardOpen}
        setIsCreateWizardOpen={setIsCreateWizardOpen}
        selectedAddressSpaces={selectedAddressSpaces}
        onSelectAddressSpace={onSelectAddressSpace}
        onSelectAllAddressSpace={onSelectAllAddressSpace}
      />
      {totalAddressSpaces > 0 && renderPagination(page, perPage)}
      {isDisplayDeleteDailogue && selectedAddressSpaces.length > 0 && (
        <DialoguePrompt
          option="Delete"
          detail={
            selectedAddressSpaces.length > 1
              ? `Are you sure you want to delete all of these address spaces: ${selectedAddressSpaces.map(
                  as => " " + as.name
                )} ?`
              : `Are you sure you want to delete this address space: ${selectedAddressSpaces[0].name} ?`
          }
          names={selectedAddressSpaces.map(as => as.name)}
          header={
            selectedAddressSpaces.length > 1
              ? "Delete these Address Spaces ?"
              : "Delete this Address Space ?"
          }
          handleCancelDialogue={handleCancelDelete}
          handleConfirmDialogue={handleConfirmDelete}
        />
      )}
    </PageSection>
  );
}
