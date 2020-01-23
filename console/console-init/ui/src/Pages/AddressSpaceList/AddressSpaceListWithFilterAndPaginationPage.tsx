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
import { ISortBy, IRowData } from "@patternfly/react-table";
import { DELETE_ADDRESS_SPACE } from "src/Queries/Queries";
import { useApolloClient } from "@apollo/react-hooks";

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
        itemCount={totalAddressSpaces}
        perPage={perPage}
        page={page}
        onSetPage={handlePageChange}
        variant="top"
        onPerPageSelect={handlePerPageChange}
      />
    );
  };
  let addressSpacesToDelete: IRowData[] = [];
  const setSelectedAddressSpacesFunc = (values: IRowData[]) => {
    const selectedData = values.filter(value => value.selected === true);
    addressSpacesToDelete = selectedData.map(data => data.originalData);
  };
  let errorData = [];
  const deleteAddressSpace = async (data: any) => {
    const deletedData = await client.mutate({
      mutation: DELETE_ADDRESS_SPACE,
      variables: {
        a: {
          Name: data.name,
          Namespace: data.nameSpace
        }
      }
    });
    if (deletedData.errors) {
      errorData.push(data);
    }
    return deletedData;
  };
  const onDeleteAll = async() => {
    if (addressSpacesToDelete && addressSpacesToDelete.length > 0)
      addressSpacesToDelete.map(addressspace =>
        deleteAddressSpace(addressspace)
      );
      setOnCreationRefetch(true);
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
        setSelectedAddressSpaces={setSelectedAddressSpacesFunc}
      />
      {totalAddressSpaces > 0 && renderPagination(page, perPage)}
    </PageSection>
  );
}
