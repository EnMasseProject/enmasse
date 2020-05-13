/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { ISortBy } from "@patternfly/react-table";
import { removeForbiddenChars } from "utils";
import { generateFilterPattern } from "./query";

const ALL_CONECTION_LIST_FILTER = (
  hostnames: any[],
  containers: any[],
  name?: string,
  namespace?: string
) => {
  let filter = "";
  let hostnamesLength = hostnames && hostnames.length;
  let containersLength = containers && containers.length;

  if (name) {
    filter += "`$.spec.addressSpace` = '" + name + "'";
  }
  if (namespace) {
    filter += " AND `$.metadata.namespace` = '" + namespace + "'";
  }
  if (hostnamesLength > 0 || (containers && containers.length > 0)) {
    filter += " AND ";
  }

  //filter hostnames
  filter += generateFilterPattern("spec.hostname", hostnames);

  if (containersLength > 0) {
    if (hostnamesLength > 0) {
      filter += " AND ";
    }
    //filter containers
    filter += generateFilterPattern("spec.containerId", containers);
  }
  return filter;
};

const ALL_CONECTION_LIST_SORT = (sortBy?: ISortBy) => {
  let orderBy = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
        orderBy = "`$.spec.hostname` ";
        break;
      case 1:
        orderBy = "`$.spec.containerId` ";
        break;
      case 2:
        orderBy = "`$.spec.protocol` ";
        break;
      case 3:
        orderBy = "`$.metadata.creationTimestamp` ";
        break;
      case 4:
        orderBy = "`$.metrics[?(@.name=='enmasse_messages_in')].value` ";
        break;
      case 5:
        orderBy = "`$.metrics[?(@.name=='enmasse_messages_out')].value` ";
        break;
      case 6:
        orderBy = "`$.metrics[?(@.name=='enmasse_senders')].value` ";
        break;
      case 7:
        orderBy = "`$.metrics[?(@.name=='enmasse_receivers')].value` ";
        break;
      default:
        break;
    }
    orderBy += sortBy.direction;
  }
  return orderBy;
};

const RETURN_ALL_CONECTION_LIST = (
  page: number,
  perPage: number,
  hostnames: any[],
  containers: any[],
  name?: string,
  namespace?: string,
  sortBy?: ISortBy
) => {
  let filter = ALL_CONECTION_LIST_FILTER(
    hostnames,
    containers,
    name,
    namespace
  );
  let orderByString = ALL_CONECTION_LIST_SORT(sortBy);

  const ALL_CONECTION_LIST = gql(
    `query all_connections_for_addressspace_view {
        connections(
          filter: "${filter}" first:${perPage} offset:${perPage *
      (page - 1)} orderBy:"${orderByString}" 
        ) {
        total
        connections {
          metadata {
            name
            creationTimestamp
          }
          spec {
            hostname
            containerId
            protocol
            encrypted
          }
          metrics {
            name
            type
            value
            units
          }
        }
      }
    }`
  );
  return ALL_CONECTION_LIST;
};

const RETURN_CONNECTION_DETAIL = (
  addressSpaceName?: string,
  addressSpaceNameSpace?: string,
  connectionName?: string
) => {
  let filter = "";
  if (addressSpaceName) {
    filter += "`$.spec.addressSpace` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpace) {
    filter += "`$.metadata.namespace` = '" + addressSpaceNameSpace + "' AND ";
  }
  if (connectionName) {
    filter += "`$.metadata.name` = '" + connectionName + "'";
  }
  const CONNECTION_DETAIL = gql`
    query single_connections {
      connections(
        filter: "${filter}" 
      ) {
        total
        connections {
          metadata {
            name
            namespace
            creationTimestamp
          }
          spec {
            hostname
            containerId
            protocol
            encrypted
            properties{
              key
              value
            }
          }
          metrics {
            name
            type
            value
            units
          }
        }
      }
    }
    `;
  return CONNECTION_DETAIL;
};

const SORT_STRING_FOR_CONNECTION_LINKS = (sortBy?: ISortBy) => {
  let orderBy = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
        orderBy = "";
        break;
      case 1:
        orderBy = "`$.metadata.name` ";
        break;
      case 2:
        orderBy = "`$.spec.address` ";
        break;
      case 3:
        orderBy = "`$.metrics[?(@.name=='enmasse_deliveries')].value` ";
        break;
      case 4:
        orderBy = "`$.metrics[?(@.name=='enmasse_accepted')].value` ";
        break;
      case 5:
        orderBy = "`$.metrics[?(@.name=='enmasse_rejected')].value` ";
        break;
      case 6:
        orderBy = "`$.metrics[?(@.name=='enmasse_released')].value` ";
        break;
      case 7:
        orderBy = "`$.metrics[?(@.name=='enmasse_modified')].value` ";
        break;
      case 8:
        orderBy = "`$.metrics[?(@.name=='enmasse_presettled')].value` ";
        break;
      case 9:
        orderBy = "`$.metrics[?(@.name=='enmasse_undelivered')].value` ";
        break;
      default:
        break;
    }
    if (sortBy.direction && orderBy !== "") {
      orderBy += sortBy.direction;
    }
  }
  return orderBy;
};

const CONNECTION_LINKS_FILTER = (
  filterNames: any[],
  filterAddresses: any[],
  addressSpaceName?: string,
  addressSpaceNameSpace?: string,
  connectionName?: string,
  filterRole?: string
) => {
  let filter = "";
  let filterForLink = "";
  let filterNamesLength = filterNames && filterNames.length;
  let filterAddressesLength = filterAddresses && filterAddresses.length;

  if (addressSpaceName) {
    filter += "`$.spec.addressSpace` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpace) {
    filter += "`$.metadata.namespace` = '" + addressSpaceNameSpace + "' AND ";
  }
  if (connectionName) {
    filter += "`$.metadata.name` = '" + connectionName + "'";
  }

  if (filterNamesLength > 0) {
    //filter for names
    filterForLink += generateFilterPattern("metadata.name", filterNames);
    if (filterAddressesLength > 0 || (filterRole && filterRole.trim() !== "")) {
      filterForLink += " AND ";
    }
  }

  if (filterAddressesLength > 0) {
    //filter addresses
    filterForLink += generateFilterPattern("spec.address", filterAddresses);
    if (filterRole && filterRole.trim() !== "") {
      filterForLink += " AND ";
    }
  }

  //filter role
  if (filterRole) {
    filterForLink += generateFilterPattern("spec.role", [
      { value: filterRole.toLowerCase(), isExact: true }
    ]);
  }
  return { filter, filterForLink };
};

const RETURN_CONNECTION_LINKS = (
  page: number,
  perPage: number,
  filterNames: any[],
  filterAddresses: any[],
  addressSpaceName?: string,
  addressSpaceNameSpace?: string,
  connectionName?: string,
  sortBy?: ISortBy,
  filterRole?: string
) => {
  const { filter, filterForLink } = CONNECTION_LINKS_FILTER(
    filterNames,
    filterAddresses,
    addressSpaceName,
    addressSpaceNameSpace,
    connectionName,
    filterRole
  );
  let orderBy = SORT_STRING_FOR_CONNECTION_LINKS(sortBy);

  const CONNECTION_DETAIL = gql`
    query single_connections {
      connections(
        filter: "${filter}" 
      ) {
        total
        connections {
          metadata {
            name
            namespace
          }
          links(first:${perPage} offset:${perPage *
    (page - 1)} orderBy:"${orderBy}"
      filter:"${filterForLink}") {
            total
            links {
              metadata {
                name
              }
              spec {
                role
                address
              }
              metrics {
                name
                type
                value
                units
              }
            }
          }
        }
      }
    }
    `;
  return CONNECTION_DETAIL;
};

const RETURN_ALL_CONNECTION_LINKS_FOR_NAME_SEARCH = (
  connectionName: string,
  namespace: string,
  name: string
) => {
  let filter = "";
  name = removeForbiddenChars(name);
  if (namespace) {
    filter += "`$.metadata.namespace` = '" + namespace + "' AND ";
  }
  if (connectionName) {
    filter += "`$.metadata.name` = '" + connectionName + "'";
  }
  const all_links = gql`
    query single_connections {
      connections(
        filter: "${filter}" 
      ) {
        total
        connections {
          links(first:10 offset:0 filter:"\`$.metadata.name\` LIKE '${name}%'") {
            total
            links {
              metadata {
                name
              }
            }
          }
        }
      }
    }
    `;
  return all_links;
};

const RETURN_ALL_CONNECTION_LINKS_FOR_ADDRESS_SEARCH = (
  connectionName: string,
  namespace: string,
  address: string
) => {
  let filter = "";
  address = removeForbiddenChars(address);
  if (namespace) {
    filter += "`$.metadata.namespace` = '" + namespace + "' AND ";
  }
  if (connectionName) {
    filter += "`$.metadata.name` = '" + connectionName + "'";
  }
  const all_links = gql`
    query single_connections {
      connections(
        filter: "${filter}" 
      ) {
        total
        connections {
          links(first:10 offset:0
                filter:"\`$.spec.address\` LIKE '${address}%'") {
            total
            links {
              spec {
                address
              }
            }
          }
        }
      }
    }
    `;
  return all_links;
};

const RETURN_ALL_CONNECTIONS_HOSTNAME_AND_CONTAINERID_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH = (
  propertyName: string,
  searchValue: string,
  name?: string,
  namespace?: string
) => {
  let filter = "";
  searchValue = removeForbiddenChars(searchValue);
  if (name) {
    filter += "`$.spec.addressSpace` = '" + name + "'";
  }
  if (namespace) {
    filter += " AND `$.metadata.namespace` = '" + namespace + "'";
  }
  if (searchValue.trim() !== "") {
    filter += " AND ";
    filter +=
      "`$.spec." + [propertyName] + "` LIKE '" + searchValue.trim() + "%' ";
  }

  const ALL_CONECTION_LIST = gql(
    `query all_connections_for_addressspace_view {
        connections(
          filter: "${filter}" first:10 offset:0
        ) {
        total
        connections {
          spec {
            hostname
            containerId
          }
        }
      }
    }`
  );
  return ALL_CONECTION_LIST;
};

export {
  RETURN_ALL_CONECTION_LIST,
  RETURN_CONNECTION_DETAIL,
  RETURN_CONNECTION_LINKS,
  RETURN_ALL_CONNECTION_LINKS_FOR_NAME_SEARCH,
  RETURN_ALL_CONNECTION_LINKS_FOR_ADDRESS_SEARCH,
  RETURN_ALL_CONNECTIONS_HOSTNAME_AND_CONTAINERID_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH
};
