/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useParams, useLocation } from "react-router";
import { useDocumentTitle, useA11yRouteChange } from "use-patternfly";
import {
  PageSection,
  PageSectionVariants,
  Grid,
  GridItem
} from "@patternfly/react-core";
import { Divider } from "@patternfly/react-core/dist/js/experimental";
import { ISortBy } from "@patternfly/react-table";
import {
  ConnectionContainer,
  ConnectionToolbarContainer
} from "modules/connection/containers";
import { TablePagination } from "components";

export default function ConnectionPage() {
  useDocumentTitle("Connection List");

  useA11yRouteChange();
  const [hostnames, setHostnames] = useState<Array<string>>([]);
  const [containerIds, setContainerIds] = useState<Array<string>>([]);
  const [totalConnections, setTotalConnections] = useState<number>(0);
  const [sortDropDownValue, setSortDropdownValue] = useState<ISortBy>();
  const { name, namespace, type } = useParams();
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;

  const renderPagination = (page: number, perPage: number) => {
    return (
      <TablePagination
        itemCount={totalConnections}
        perPage={perPage}
        page={page}
        variant="top"
      />
    );
  };

  return (
    <PageSection variant={PageSectionVariants.light}>
      <Grid>
        <GridItem span={6}>
          <ConnectionToolbarContainer
            selectedHostnames={hostnames}
            setSelectedHostnames={setHostnames}
            selectedContainers={containerIds}
            setSelectedContainers={setContainerIds}
            totalRecords={totalConnections}
            sortValue={sortDropDownValue}
            setSortValue={setSortDropdownValue}
            namespace={namespace || ""}
            addressSpaceName={name || ""}
          />
        </GridItem>
        <GridItem span={6}>{renderPagination(page, perPage)}</GridItem>
      </Grid>
      <Divider />
      <ConnectionContainer
        name={name}
        namespace={namespace}
        hostnames={hostnames}
        containerIds={containerIds}
        setTotalConnections={setTotalConnections}
        page={page}
        perPage={perPage}
        sortValue={sortDropDownValue}
        setSortValue={setSortDropdownValue}
        addressSpaceType={type}
      />
      {renderPagination(page, perPage)}
    </PageSection>
  );
}
