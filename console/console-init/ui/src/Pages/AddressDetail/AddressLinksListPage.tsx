/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_ADDRESS_LINKS } from "src/Queries/Queries";
import { IAddressLinksResponse } from "src/Types/ResponseTypes";
import { Loading } from "use-patternfly";
import { IClient, ClientList } from "src/Components/AddressDetail/ClientList";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { EmptyLinks } from "src/Components/Common/EmptyLinks";
import { ISortBy } from "@patternfly/react-table";

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
  if (sortValue && sortBy != sortValue) {
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
    { pollInterval: 20000 }
  );
  if (loading) return <Loading />;
  if (error) console.log(error);
  const { addresses } = data || {
    addresses: { Total: 0, Addresses: [] }
  };
  if (
    addresses &&
    addresses.Addresses.length > 0 &&
    addresses.Addresses[0].Links.Total > 0
  ) {
    setAddressLinksTotal(addresses.Addresses[0].Links.Total);
  }
  const links =
    addresses &&
    addresses.Addresses.length > 0 &&
    addresses.Addresses[0].Links.Total > 0 &&
    addresses.Addresses[0].Links;

  console.log(links);
  let clientRows: IClient[] = addresses.Addresses[0].Links.Links.map(link => ({
    role: link.Spec.Role.toString(),
    containerId: link.Spec.Connection.Spec.ContainerId,
    name: link.ObjectMeta.Name,
    deliveryRate: getFilteredValue(link.Metrics, link.Spec.Role === "sender" ? "enmasse_messages_in" : "enmasse_messages_out"),
    backlog: getFilteredValue(link.Metrics, "enmasse_messages_backlog"),
    connectionName: link.Spec.Connection.ObjectMeta.Name,
    addressSpaceName: name,
    addressSpaceNamespace: namespace,
    addressSpaceType: type
  }));
  const onSort = (_event: any, index: any, direction: any) => {
    setSortBy({ index: index, direction: direction });
    setSortValue({index:index,direction:direction});
  };
  return (
    <>
      {links && links.Total > 0 ? (
        <ClientList rows={clientRows} onSort={onSort} sortBy={sortBy} />
      ) : (
        <EmptyLinks />
      )}
    </>
  );
};
