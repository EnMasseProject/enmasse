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
import { AddressListPage } from "./AddressListPage";
import { Divider } from "@patternfly/react-core/dist/js/experimental";
import { useQuery } from "@apollo/react-hooks";
import { CURRENT_ADDRESS_SPACE_PLAN } from "src/Queries/Queries";
import { ISortBy } from "@patternfly/react-table";

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
    AddressSpaces: Array<{
      Spec: {
        Plan: {
          ObjectMeta: {
            Name: string;
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
  const [filterValue, setFilterValue] = React.useState<string | null>("Name");
  const [filterNames, setFilterNames] = React.useState<any[]>([]);
  const [typeValue, setTypeValue] = React.useState<string | null>(null);
  const [statusValue, setStatusValue] = React.useState<string | null>(null);
  const [totalAddresses, setTotalAddress] = React.useState<number>(0);
  const [addressSpacePlan, setAddressSpacePlan] = React.useState<string | null>(
    null
  );
  const location = useLocation();
  const history = useHistory();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [sortDropDownValue, setSortDropdownValue] = React.useState<ISortBy>();
  const [isCreateWizardOpen, setIsCreateWizardOpen] = React.useState(false);

  const { data } = useQuery<IAddressSpacePlanResponse>(
    CURRENT_ADDRESS_SPACE_PLAN(name, namespace)
  );

  const { addressSpaces } = data || {
    addressSpaces: { AddressSpaces: [] }
  };

  if (!addressSpacePlan && addressSpaces.AddressSpaces[0]) {
    setAddressSpacePlan(
      addressSpaces.AddressSpaces[0].Spec.Plan.ObjectMeta.Name
    );
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
            setSortValue={setSortDropdownValue}
            isCreateWizardOpen={isCreateWizardOpen}
            setIsCreateWizardOpen={setIsCreateWizardOpen}
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
      />
      {totalAddresses > 0 && renderPagination(page, perPage)}
    </PageSection>
  );
}
