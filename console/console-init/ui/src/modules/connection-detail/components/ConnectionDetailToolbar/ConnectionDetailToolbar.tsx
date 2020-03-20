/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  PageSection,
  PageSectionVariants,
  Title,
  GridItem,
  Grid
} from "@patternfly/react-core";
import { GridStylesForTableHeader } from "modules/address/AddressPage";
import { ConnectionDetailContainer } from "../../containers/ConnectionDetailContainer";
import { useLocation } from "react-router";
import { css } from "@patternfly/react-styles";
import { ConnectionDetailFilter } from "modules/connection-detail/components/ConnectionDetailFilter/ConnectionDetailFilter";
import { ISortBy } from "@patternfly/react-table";
import { Divider } from "@patternfly/react-core/dist/js/experimental";
import { TablePagination } from "components";
interface IConnectionDetailToolbarProps {
  name?: string;
  namespace?: string;
  connectionName?: string;
}
export const ConnectionDetailToolbar: React.FunctionComponent<IConnectionDetailToolbarProps> = ({
  name,
  namespace,
  connectionName
}) => {
  const location = useLocation();
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
        size={"lg"}
        className={css(GridStylesForTableHeader.filter_left_margin)}
      >
        Links for connection - {connectionName}
      </Title>
      <br />
      <Grid>
        <GridItem span={6}>
          <ConnectionDetailFilter
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
            namespace={namespace || ""}
            connectionName={connectionName || ""}
          />
        </GridItem>
        <GridItem span={6}>{renderPagination(page, perPage)}</GridItem>
      </Grid>
      <Divider />
      <ConnectionDetailContainer
        name={name || ""}
        namespace={namespace || ""}
        connectionName={connectionName || ""}
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
