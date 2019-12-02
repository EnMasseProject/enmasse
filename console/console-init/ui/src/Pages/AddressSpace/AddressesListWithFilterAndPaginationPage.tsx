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

function AddressesListFunction() {
  useDocumentTitle("Address List");
  useA11yRouteChange();
  const { name, namespace } = useParams();
  const [inputValue, setInputValue] = React.useState<string | null>(null);
  const [filterValue, setFilterValue] = React.useState<string | null>(null);
  const [typeValue, setTypeValue] = React.useState<string | null>(null);
  const [statusValue, setStatusValue] = React.useState<string | null>(null);
  const [totalAddresses, setTotalAddress] = React.useState<number>(0);
  const location = useLocation();
  const history = useHistory();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 0;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;

  const setSearchParam = React.useCallback(
    (name: string, value: string) => {
      searchParams.set(name, value.toString());
    },
    [searchParams]
  );

  const handlePageChange = React.useCallback(
    (event: React.SyntheticEvent, newPage: number) => {
      setSearchParam("page", (newPage - 1).toString());

      history.push({
        search: searchParams.toString()
      });
    },
    [setSearchParam, history, searchParams]
  );

  const handlePerPageChange = React.useCallback(
    (
      event: React.MouseEvent | React.KeyboardEvent | MouseEvent,
      newPerPage: number
    ) => {
      setSearchParam("page", "0");
      setSearchParam("perPage", newPerPage.toString());
      history.push({
        search: searchParams.toString()
      });
    },
    [setSearchParam, history, searchParams]
  );
  return (
    <PageSection variant={PageSectionVariants.light}>
      <Grid className={css(GridStylesForTableHeader.grid_bottom_border)}>
        <GridItem span={7}>
          <AddressListFilterPage
            inputValue={inputValue}
            setInputValue={setInputValue}
            filterValue={filterValue}
            setFilterValue={setFilterValue}
            typeValue={typeValue}
            setTypeValue={setTypeValue}
            statusValue={statusValue}
            setStatusValue={setStatusValue}
          />
        </GridItem>
        <GridItem span={5}>
          {totalAddresses > 0 && (
            <Pagination
              itemCount={totalAddresses}
              perPage={perPage}
              page={page}
              onSetPage={handlePageChange}
              variant="top"
              onPerPageSelect={handlePerPageChange}
            />
          )}
        </GridItem>
      </Grid>
      <AddressListPage
        name={name}
        namespace={namespace}
        inputValue={inputValue}
        filterValue={filterValue}
        typeValue={typeValue}
        statusValue={statusValue}
        setTotalAddress={setTotalAddress}
        page={page}
        perPage={perPage}
      />
      {totalAddresses > 0 && (
        <Pagination
          itemCount={totalAddresses}
          perPage={perPage}
          page={page}
          onSetPage={handlePageChange}
          variant="top"
          onPerPageSelect={handlePerPageChange}
        />
      )}
    </PageSection>
  );
}

export default function AddressesListPage() {
  return <AddressesListFunction />;
}
