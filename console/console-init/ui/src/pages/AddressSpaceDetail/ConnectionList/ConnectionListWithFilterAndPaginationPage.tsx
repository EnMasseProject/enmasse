/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { useDocumentTitle, useA11yRouteChange } from "use-patternfly";
import { useParams, useHistory, useLocation } from "react-router";
import {
  PageSection,
  PageSectionVariants,
  Grid,
  GridItem,
  Pagination
} from "@patternfly/react-core";
import { ConnectionsListPage } from "./ConnectionsListPage";
import { Divider } from "@patternfly/react-core/dist/js/experimental";
import { ConnectionListFilter } from "pages/AddressSpaceDetail/ConnectionList/ConnectionListFilter";
import { ISortBy } from "@patternfly/react-table";

const ConnectionListFunction = () => {
  useDocumentTitle("Connection List");

  useA11yRouteChange();
  const { name, namespace } = useParams();
  const [filterValue, setFilterValue] = React.useState<string>("Hostname");
  const [hostnames, setHostnames] = React.useState<Array<any>>([]);
  const [containerIds, setContainerIds] = React.useState<Array<any>>([]);
  const [totalConnections, setTotalConnections] = React.useState<number>(0);
  const location = useLocation();
  const history = useHistory();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [sortDropDownValue, setSortDropdownValue] = React.useState<ISortBy>();

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
        itemCount={totalConnections}
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
        <GridItem span={6}>
          <ConnectionListFilter
            filterValue={filterValue}
            setFilterValue={setFilterValue}
            hostnames={hostnames}
            setHostnames={setHostnames}
            containerIds={containerIds}
            setContainerIds={setContainerIds}
            totalConnections={totalConnections}
            sortValue={sortDropDownValue}
            setSortValue={setSortDropdownValue}
            addressSpaceName={name}
            namespace={namespace}
          />
        </GridItem>
        <GridItem span={6}>
          {totalConnections > 0 && renderPagination(page, perPage)}
        </GridItem>
      </Grid>
      <Divider />
      <ConnectionsListPage
        name={name}
        namespace={namespace}
        hostnames={hostnames}
        containerIds={containerIds}
        setTotalConnections={setTotalConnections}
        page={page}
        perPage={perPage}
        sortValue={sortDropDownValue}
        setSortValue={setSortDropdownValue}
      />
      {totalConnections > 0 && renderPagination(page, perPage)}
    </PageSection>
  );
};

export default function ConnectionListWithFilterAndPaginationPage() {
  return <ConnectionListFunction />;
}
