/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  PageSection,
  PageSectionVariants,
  Title,
  Pagination,
  GridItem,
  Grid
} from "@patternfly/react-core";
import { GridStylesForTableHeader } from "../AddressSpaceDetail/AddressList/AddressesListWithFilterAndPaginationPage";
import { ConnectionLinksListPage } from "./ConnectionsLinksListPage";
import { useLocation, useHistory } from "react-router";
import { css } from "@patternfly/react-styles";
import { ConnectionLinksFilter } from "src/Pages/ConnectionDetail/ConnectionLinksFilter";
import { ISortBy } from "@patternfly/react-table";
interface IConnectionLinksWithFilterAndPaginationPageProps {
  name?: string;
  namespace?: string;
  connectionname?: string;
}
export const ConnectionLinksWithFilterAndPaginationPage: React.FunctionComponent<IConnectionLinksWithFilterAndPaginationPageProps> = ({
  name,
  namespace,
  connectionname
}) => {
  const location = useLocation();
  const history = useHistory();
  const searchParams = new URLSearchParams(location.search);
  const [totalLinks, setTotalLinks] = React.useState<number>(0);
  const page = parseInt(searchParams.get("page") || "", 10) || 0;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [filterValue, setFilterValue] = React.useState<string>("Name");
  const [filterNames, setFilterNames] = React.useState<Array<string>>([]);
  const [filterAddresses, setFilterAddresses] = React.useState<Array<string>>(
    []
  );
  const [filterRole, setFilterRole] = React.useState<string>();
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
      setSearchParam("page", "0");
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
        itemCount={totalLinks}
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
      <Title
        size={"lg"}
        className={css(GridStylesForTableHeader.filter_left_margin)}
      >
        Links
      </Title>
      <br />
      <Grid>
        <GridItem span={6}>
          <ConnectionLinksFilter
            filterValue={filterValue}
            setFilterValue={setFilterValue}
            filterNames={filterNames}
            setFilterNames={setFilterNames}
            filterAddresses={filterAddresses}
            setFilterAddresses={setFilterAddresses}
            filterRole={filterRole}
            setFilterRole={setFilterRole}
            totalLinks={totalLinks}
            sortValue={sortDropDownValue}
            setSortValue={setSortDropdownValue}
            addressSpaceName={name || ""}
            namespace={namespace || ""}
            connectionName={connectionname || ""}
          />
        </GridItem>
        <GridItem span={6}>{renderPagination(page, perPage)}</GridItem>
      </Grid>
      <ConnectionLinksListPage
        name={name || ""}
        namespace={namespace || ""}
        connectionName={connectionname || ""}
        page={page}
        perPage={perPage}
        setTotalLinks={setTotalLinks}
        filterNames={filterNames}
        filterAddresses={filterAddresses}
        filterRole={filterRole}
        sortValue={sortDropDownValue}
        setSortValue={setSortDropdownValue}
      />
      {renderPagination(page, perPage)}
    </PageSection>
  );
};
