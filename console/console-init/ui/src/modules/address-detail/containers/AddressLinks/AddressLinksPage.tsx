/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useLocation } from "react-router";
import {
  PageSection,
  PageSectionVariants,
  Title,
  Grid,
  GridItem
} from "@patternfly/react-core";
import { ISortBy } from "@patternfly/react-table";
import { Divider } from "@patternfly/react-core/dist/js/experimental";
import { css } from "@patternfly/react-styles";
import { GridStylesForTableHeader } from "modules/address/AddressPage";
import { AddressLinksContainer } from "modules/address-detail/containers/AddressLinksContainer";
import { AddressLinksToolbar } from "modules/address-detail/containers/AddressLinksToolbar";
import { TablePagination } from "components/TablePagination";
import { IFilterValue } from "modules/address/utils";

interface IAddressLinksListPageProps {
  addressspace_name: string;
  addressspace_namespace: string;
  addressspace_type: string;
  addressName: string;
  addressDisplayName: string;
}

const AddressLinksPage: React.FunctionComponent<IAddressLinksListPageProps> = ({
  addressspace_name,
  addressspace_namespace,
  addressspace_type,
  addressName,
  addressDisplayName
}) => {
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [addresLinksTotal, setAddressLinksTotal] = useState<number>(0);
  const [filterValue, setFilterValue] = useState<string>("Name");
  const [filterNames, setFilterNames] = useState<Array<IFilterValue>>([]);
  const [sortDropDownValue, setSortDropdownValue] = useState<ISortBy>();
  const [filterContainers, setFilterContainers] = useState<Array<IFilterValue>>(
    []
  );
  const [filterRole, setFilterRole] = useState<string>();
  const renderPagination = () => {
    return (
      <TablePagination
        itemCount={addresLinksTotal}
        variant={"top"}
        page={page}
        perPage={perPage}
      />
    );
  };
  return (
    <PageSection>
      <PageSection variant={PageSectionVariants.light}>
        <Title
          size={"lg"}
          className={css(GridStylesForTableHeader.filter_left_margin)}
        >
          Links for address - {addressDisplayName}
        </Title>
        <Grid>
          <GridItem span={7}>
            <AddressLinksToolbar
              filterValue={filterValue}
              setFilterValue={setFilterValue}
              filterNames={filterNames}
              setFilterNames={setFilterNames}
              filterContainers={filterContainers}
              setFilterContainers={setFilterContainers}
              filterRole={filterRole}
              setFilterRole={setFilterRole}
              totalLinks={addresLinksTotal}
              sortValue={sortDropDownValue}
              setSortValue={setSortDropdownValue}
              namespace={addressspace_namespace}
              addressName={addressName}
            />
          </GridItem>
          <GridItem span={5}>{renderPagination()}</GridItem>
        </Grid>
        <Divider />
        <AddressLinksContainer
          page={page}
          perPage={perPage}
          name={addressspace_name}
          namespace={addressspace_namespace}
          addressName={addressName}
          setAddressLinksTotal={setAddressLinksTotal}
          type={addressspace_type}
          filterNames={filterNames}
          filterContainers={filterContainers}
          sortValue={sortDropDownValue}
          setSortValue={setSortDropdownValue}
          filterRole={filterRole}
        />
        {renderPagination()}
      </PageSection>
    </PageSection>
  );
};

export { AddressLinksPage };
