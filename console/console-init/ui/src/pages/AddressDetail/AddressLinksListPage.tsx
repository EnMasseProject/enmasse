/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_ADDRESS_LINKS } from "queries";
import { IAddressLinksResponse } from "types/ResponseTypes";
import { IClient, ClientList } from "components/AddressDetail/ClientList";
import { getFilteredValue } from "components/common/ConnectionListFormatter";
import { EmptyLinks } from "components/common/EmptyLinks";
import { ISortBy } from "@patternfly/react-table";
import { Loading } from "use-patternfly";
import { POLL_INTERVAL } from "constants/constants";

export interface IAddressLinksListProps {
  page: number;
  perPage: number;
  name?: string;
  namespace?: string;
  addressName?: string;
  type?: string;
  setAddressLinksTotal: (total: number) => void;
  filterNames: string[];
  filterContainers: string[];
  sortValue?: ISortBy;
  setSortValue: (value?: ISortBy) => void;
  filterRole?: string;
}
export const AddressLinksListPage: React.FunctionComponent<IAddressLinksListProps> = ({
  page,
  perPage,
  name,
  namespace,
  addressName,
  type,
  setAddressLinksTotal,
  filterNames,
  filterContainers,
  sortValue,
  setSortValue,
  filterRole
}) => {
  const [sortBy, setSortBy] = React.useState<ISortBy>();
  if (sortValue && sortBy !== sortValue) {
    setSortBy(sortValue);
  }
  const { loading, error, data } = useQuery<IAddressLinksResponse>(
    RETURN_ADDRESS_LINKS(
      page,
      perPage,
      filterNames,
      filterContainers,
      name,
      namespace,
      addressName,
      sortBy,
      filterRole
    ),
    { pollInterval: POLL_INTERVAL }
  );
  if (loading && !data) return <Loading />;
  if (error) console.log(error);
  const { addresses } = data || {
    addresses: { total: 0, addresses: [] }
  };
  if (
    addresses &&
    addresses.addresses.length > 0 &&
    addresses.addresses[0].links.total >= 0
  ) {
    setAddressLinksTotal(addresses.addresses[0].links.total);
  }
  const links =
    addresses &&
    addresses.addresses.length > 0 &&
    addresses.addresses[0].links.total > 0 &&
    addresses.addresses[0].links;

  let clientRows: IClient[] = addresses.addresses[0].links.links.map(link => ({
    role: link.spec.role.toString(),
    containerId: link.spec.connection.spec.containerId,
    name: link.metadata.name,
    deliveryRate: getFilteredValue(
      link.metrics,
      link.spec.role === "sender"
        ? "enmasse_messages_in"
        : "enmasse_messages_out"
    ),
    backlog: getFilteredValue(link.metrics, "enmasse_messages_backlog"),
    connectionName: link.spec.connection.metadata.name,
    addressSpaceName: name,
    addressSpaceNamespace: namespace,
    addressSpaceType: type
  }));
  const onSort = (_event: any, index: any, direction: any) => {
    setSortBy({ index: index, direction: direction });
    setSortValue({ index: index, direction: direction });
  };
  return (
    <>
      <ClientList rows={clientRows} onSort={onSort} sortBy={sortBy} />
      {links && links.total > 0 ? <></> : <EmptyLinks />}
    </>
  );
};
