/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  PageSection,
  PageSectionVariants,
  Title,
  GridItem,
  Grid,
  Divider
} from "@patternfly/react-core";
import { useLocation } from "react-router";
import { css } from "aphrodite";
import { ISortBy } from "@patternfly/react-table";
import { GridStylesForTableHeader } from "modules/address/AddressPage";
import { ConnectionLinksContainer } from "modules/connection-detail/containers";
import { TablePagination } from "components";
import { ConnectionLinksToolbarContainer } from "modules/connection-detail/containers";
interface IConnectionDetailToolbarProps {
  name?: string;
  namespace?: string;
  connectionName?: string;
}
export const ConnectionLinksWithToolbar: React.FunctionComponent<IConnectionDetailToolbarProps> = ({
  name,
  namespace,
  connectionName
}) => {
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const [totalLinks, setTotalLinks] = useState<number>(0);
  const page = parseInt(searchParams.get("page") || "", 10) || 0;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [filterNames, setFilterNames] = useState<Array<string>>([]);
  const [filterAddresses, setFilterAddresses] = useState<Array<string>>([]);
  const [filterRole, setFilterRole] = useState<string | null>();
  const [sortDropDownValue, setSortDropdownValue] = useState<ISortBy>();

  const renderPagination = (page: number, perPage: number) => {
    return (
      <TablePagination
        itemCount={totalLinks}
        perPage={perPage}
        page={page}
        variant="top"
      />
    );
  };

  return (
    <PageSection variant={PageSectionVariants.light}>
      <Title
        headingLevel="h2"
        size="lg"
        className={css(GridStylesForTableHeader.filter_left_margin)}
      >
        Links for connection - {connectionName}
      </Title>
      <br />
      <Grid>
        <GridItem span={6}>
          <ConnectionLinksToolbarContainer
            selectedNames={filterNames}
            setSelectedNames={setFilterNames}
            selectedAddresses={filterAddresses}
            setSelectedAddresses={setFilterAddresses}
            roleSelected={filterRole}
            setRoleSelected={setFilterRole}
            totalRecords={totalLinks}
            sortValue={sortDropDownValue}
            setSortValue={setSortDropdownValue}
            namespace={namespace || ""}
            connectionName={connectionName || ""}
          />
        </GridItem>
        <GridItem span={6}>{renderPagination(page, perPage)}</GridItem>
      </Grid>
      <Divider />
      <ConnectionLinksContainer
        name={name || ""}
        namespace={namespace || ""}
        connectionName={connectionName || ""}
        page={page}
        perPage={perPage}
        setTotalLinks={setTotalLinks}
        filterNames={filterNames}
        filterAddresses={filterAddresses}
        filterRole={filterRole || undefined}
        sortValue={sortDropDownValue}
        setSortValue={setSortDropdownValue}
      />
      {renderPagination(page, perPage)}
    </PageSection>
  );
};
