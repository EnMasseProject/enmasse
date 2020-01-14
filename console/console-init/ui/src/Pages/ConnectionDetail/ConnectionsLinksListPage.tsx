/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { useQuery } from "@apollo/react-hooks";
import { IConnectionLinksResponse } from "src/Types/ResponseTypes";
import { RETURN_CONNECTION_LINKS } from "src/Queries/Queries";
import { Loading } from "use-patternfly";
import { ILink, LinkList } from "src/Components/ConnectionDetail/LinkList";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { ISortBy } from "@patternfly/react-table";

interface IConnectionLinksListPageProps {
  name: string;
  namespace: string;
  connectionName: string;
  page: number;
  perPage: number;
  setTotalLinks: (value: number) => void;
  filterNames: string[];
  filterAddresses: string[];
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
  if (sortValue && sortBy != sortValue) {
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
    { pollInterval: 20000 }
  );
  if (loading) return <Loading />;
  if (error) {
    console.log(error);
    return <Loading />;
  }
  const { connections } = data || {
    connections: { Total: 0, Connections: [] }
  };
  const connection = connections.Connections[0];
  console.log(connection.Links.Links);
  let linkRows: ILink[] = [];
  if (connection && connection.Links.Total > 0) {
    setTotalLinks(connection.Links.Total);
    linkRows = connection.Links.Links.map(link => ({
      name: link.ObjectMeta.Name,
      role: link.Spec.Role,
      address: link.Spec.Address,
      deliveries: getFilteredValue(link.Metrics, "enmasse_deliveries"),
      rejected: getFilteredValue(link.Metrics, "enmasse_rejected"),
      released: getFilteredValue(link.Metrics, "enmasse_released"),
      modified: getFilteredValue(link.Metrics, "enmasse_modified"),
      presettled: getFilteredValue(link.Metrics, "enmasse_presettled"),
      undelivered: getFilteredValue(link.Metrics, "enmasse_undelivered")
    }));
  }

  const onSort = (_event: any, index: any, direction: any) => {
    setSortBy({ index: index, direction: direction });
    setSortValue({ index: index, direction: direction });
  };

  return (
    <>
      <LinkList rows={linkRows} onSort={onSort} sortBy={sortBy} />
    </>
  );
};
