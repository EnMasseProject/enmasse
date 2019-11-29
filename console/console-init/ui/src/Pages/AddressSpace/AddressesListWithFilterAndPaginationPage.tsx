import * as React from "react";
import { useParams } from "react-router-dom";

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
              perPage={10}
              page={1}
              onSetPage={() => {}}
              widgetId="pagination-options-menu-top"
              onPerPageSelect={() => {}}
              style={{ paddingTop: 16, paddingBottom: 16 }}
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
      />
      {totalAddresses > 0 && (
        <Pagination
          itemCount={totalAddresses}
          perPage={10}
          page={1}
          onSetPage={() => {}}
          widgetId="pagination-options-menu-top"
          onPerPageSelect={() => {}}
        />
      )}
    </PageSection>
  );
}

export default function AddressesListPage() {
  return <AddressesListFunction />;
}
