/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { useQuery } from "@apollo/react-hooks";
import { IConnectionLinksResponse } from "types/ResponseTypes";
import { RETURN_CONNECTION_LINKS } from "queries";
import { Loading } from "use-patternfly";
import { ILink, LinkList } from "components/ConnectionDetail/LinkList";
import { getFilteredValue } from "components/common/ConnectionListFormatter";
import { ISortBy } from "@patternfly/react-table";
import { POLL_INTERVAL, FetchPolicy } from "constants/constants";
import { EmptyLinks } from "components/common/EmptyLinks";

interface IConnectionLinksListPageProps {
  name: string;
  namespace: string;
  connectionName: string;
  page: number;
  perPage: number;
  setTotalLinks: (value: number) => void;
  filterNames: any[];
  filterAddresses: any[];
  filterRole?: string;
  sortValue?: ISortBy;
  setSortValue: (value: ISortBy) => void;
}
export const ConnectionLinksListPage: React.FunctionComponent<IConnectionLinksListPageProps> = ({
  name,
  namespace,
  connectionName,
  page,
  perPage,
  setTotalLinks,
  filterNames,
  filterAddresses,
  filterRole,
  sortValue,
  setSortValue
}) => {
  const [sortBy, setSortBy] = React.useState<ISortBy>();
  if (sortValue && sortBy !== sortValue) {
    setSortBy(sortValue);
  }
  const { loading, error, data } = useQuery<IConnectionLinksResponse>(
    RETURN_CONNECTION_LINKS(
      page,
      perPage,
      filterNames,
      filterAddresses,
      name || "",
      namespace || "",
      connectionName || "",
      sortBy,
      filterRole
    ),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );
  if (loading && !data) return <Loading />;
  if (error) {
    console.log(error);
  }
  const { connections } = data || {
    connections: { total: 0, connections: [] }
  };
  const connection = connections.connections[0];
  let linkRows: ILink[] = [];
  if (connection && connection.links.total >= 0) {
    setTotalLinks(connection.links.total);
    linkRows = connection.links.links.map(link => ({
      name: link.metadata.name,
      role: link.spec.role,
      address: link.spec.address,
      deliveries: getFilteredValue(link.metrics, "enmasse_deliveries"),
      accepted: getFilteredValue(link.metrics, "enmasse_accepted"),
      rejected: getFilteredValue(link.metrics, "enmasse_rejected"),
      released: getFilteredValue(link.metrics, "enmasse_released"),
      modified: getFilteredValue(link.metrics, "enmasse_modified"),
      presettled: getFilteredValue(link.metrics, "enmasse_presettled"),
      undelivered: getFilteredValue(link.metrics, "enmasse_undelivered")
    }));
  }

  const onSort = (_event: any, index: any, direction: any) => {
    setSortBy({ index: index, direction: direction });
    setSortValue({ index: index, direction: direction });
  };

  return (
    <>
      <LinkList rows={linkRows} onSort={onSort} sortBy={sortBy} />
      {linkRows && linkRows.length > 0 ? <></> : <EmptyLinks />}
    </>
  );
};
