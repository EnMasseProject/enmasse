import * as React from "react";
import {
  PageSection,
  PageSectionVariants,
  Title,
  Pagination
} from "@patternfly/react-core";
import { GridStylesForTableHeader } from "../AddressSpaceDetail/AddressList/AddressesListWithFilterAndPaginationPage";
import { AddressLinksListPage } from "./AddressLinksListPage";
import { useHistory, useLocation } from "react-router";
import { css } from "emotion";
interface IAddressLinksWithFilterAndPaginationProps {
  addressspace_name: string;
  addressspace_namespace: string;
  addressspace_type: string;
  addressname: string;
}
export const AddressLinksWithFilterAndPagination: React.FunctionComponent<IAddressLinksWithFilterAndPaginationProps> = ({
  addressspace_name,
  addressspace_namespace,
  addressspace_type,
  addressname
}) => {
  const location = useLocation();
  const history = useHistory();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const [addresLinksTotal, setAddressLinksTotal] = React.useState<number>(0);

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
        itemCount={addresLinksTotal}
        perPage={perPage}
        page={page}
        onSetPage={handlePageChange}
        variant="top"
        onPerPageSelect={handlePerPageChange}
      />
    );
  };
  return (
    <PageSection>
      <PageSection variant={PageSectionVariants.light}>
        <Title
          size={"lg"}
          className={css(GridStylesForTableHeader.filter_left_margin)}>
          Clients
        </Title>
        {addresLinksTotal > 0 && renderPagination(page, perPage)}
        <AddressLinksListPage
          page={page}
          perPage={perPage}
          name={addressspace_name}
          namespace={addressspace_namespace}
          addressname={addressname}
          setAddressLinksTotal={setAddressLinksTotal}
          type={addressspace_type}
        />
        {addresLinksTotal > 0 && renderPagination(page, perPage)}
      </PageSection>
    </PageSection>
  );
};
