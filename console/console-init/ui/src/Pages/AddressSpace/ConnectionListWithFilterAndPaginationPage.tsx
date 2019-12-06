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
import { GridStylesForTableHeader } from "./AddressesListWithFilterAndPaginationPage";
import { ConnectionListFilterPage } from "./ConnectionListFilterPage";
import { css } from "@patternfly/react-styles";
import { ConnectionsListPage } from "./ConnectionsListPage";

const ConnectionListFunction = () => {
  useDocumentTitle("Connection List");

  useA11yRouteChange();
  const { name, namespace, type } = useParams();
  const [filterValue, setFilterValue] = React.useState<string>("Hostname");
  const [filterIsExpanded, setFilterIsExpanded] = React.useState(false);
  const [hosts, setHosts] = React.useState<Array<string>>([]);
  const [containerIds, setContainerIds] = React.useState<Array<string>>([]);
  const [totalConnections, setTotalConnections] = React.useState<number>(0);
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
      <Grid className={css(GridStylesForTableHeader.grid_bottom_border)}>
        <GridItem span={6}>
          <ConnectionListFilterPage
            filterValue={filterValue}
            setFilterValue={setFilterValue}
            filterIsExpanded={filterIsExpanded}
            setFilterIsExpanded={setFilterIsExpanded}
            hosts={hosts}
            setHosts={setHosts}
            containerIds={containerIds}
            setContainerIds={setContainerIds}
          />
        </GridItem>
        <GridItem span={6}>
          {totalConnections > 0 && renderPagination(page, perPage)}
        </GridItem>
      </Grid>
      <ConnectionsListPage
        name={name}
        namespace={namespace}
        addressSpaceType={type}
        hosts={hosts}
        containerIds={containerIds}
        setTotalConnections={setTotalConnections}
        page={page}
        perPage={perPage}
      />
      {totalConnections > 0 && renderPagination(page, perPage)}
    </PageSection>
  );
};

export default function ConnectionListWithFilterAndPaginationPage() {
  return <ConnectionListFunction />;
}
