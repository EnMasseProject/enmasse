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
import { css, StyleSheet } from "@patternfly/react-styles";
import { AddressListFilterPage } from "./AddressListFilterPage";
import { AddressListPage } from "./AddressListPage";

export const GridStylesForTableHeader = StyleSheet.create({
  grid_bottom_border: {
    paddingBottom: "1em",
    borderBottom: "0.05em solid",
    borderBottomColor: "lightgrey"
  },
  filter_left_margin: {
    marginLeft: 24
  },
  create_button_left_margin: {
    marginLeft: 10
  }
});

export default function AddressesList() {
  useDocumentTitle("Address List");
  useA11yRouteChange();
  const { name, namespace,type } = useParams();
  const [filterValue, setFilterValue] = React.useState<string | null>(null);
  const [filterNames, setFilterNames] = React.useState<string[]>([]);
  const [typeValue, setTypeValue] = React.useState<string | null>(null);
  const [statusValue, setStatusValue] = React.useState<string | null>(null);
  const [totalAddresses, setTotalAddress] = React.useState<number>(0);
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
      <Grid className={css(GridStylesForTableHeader.grid_bottom_border)}>
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
          />
        </GridItem>
        <GridItem span={5}>
          {totalAddresses > 0 && renderPagination(page, perPage)}
        </GridItem>
      </Grid>
      <AddressListPage
        name={name}
        namespace={namespace}
        filterNames={filterNames}
        typeValue={typeValue}
        statusValue={statusValue}
        setTotalAddress={setTotalAddress}
        page={page}
        perPage={perPage}
        addressSpaceType={type}
      />
      {totalAddresses > 0 && renderPagination(page, perPage)}
    </PageSection>
  );
}
