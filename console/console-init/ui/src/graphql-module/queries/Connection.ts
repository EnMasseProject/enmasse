/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { ISortBy } from "@patternfly/react-table";

const ALL_CONECTION_LIST_FILTER = (
  hostnames: any[],
  containers: any[],
  name?: string,
  namespace?: string
) => {
  let filter = "";
  let hostnamesLength = hostnames && hostnames.length;
  let hostname = hostnames && hostnames[0];
  let hostnameValue = hostname && hostname.value && hostname.value.trim();

  let containersLength = containers && containers.length;
  let container = containers && containers[0];
  let containerValue = container && container.value && container.value.trim();

  if (name) {
    filter += "`$.spec.addressSpace` = '" + name + "'";
  }
  if (namespace) {
    filter += " AND `$.metadata.namespace` = '" + namespace + "'";
  }
  if (hostnamesLength > 0 || (containers && containers.length > 0)) {
    filter += " AND ";
  }
  if (hostnamesLength > 0) {
    if (hostnamesLength > 1) {
      if (hostname.isExact)
        filter += "(`$.spec.hostname` = '" + hostnameValue + "'";
      else filter += "(`$.spec.hostname` LIKE '" + hostnameValue + "%' ";
      for (let i = 1; i < hostnamesLength; i++) {
        let hostname = hostnames && hostnames[i];
        let hostnameValue = hostname && hostname.value && hostname.value.trim();
        if (hostname.isExact)
          filter += "OR `$.spec.hostname` = '" + hostnameValue + "'";
        else filter += "OR `$.spec.hostname` LIKE '" + hostnameValue + "%' ";
      }
      filter += ")";
    } else {
      if (hostname.isExact)
        filter += "(`$.spec.hostname` = '" + hostnameValue + "')";
      else filter += "(`$.spec.hostname` LIKE '" + hostnameValue + "%')";
    }
  }

  if (containersLength > 0) {
    if (hostnamesLength > 0) {
      filter += " AND ";
    }
    if (containersLength > 1) {
      if (container.isExact)
        filter += "(`$.spec.containerId` = '" + containerValue + "'";
      else filter += "(`$.spec.containerId` LIKE '" + containerValue + "%' ";
      for (let i = 1; i < containersLength; i++) {
        let container = containers && containers[i];
        let containerValue =
          container && container.value && container.value.trim();
        if (container.isExact)
          filter += "OR `$.spec.containerId` = '" + containerValue + "'";
        else
          filter += "OR `$.spec.containerId` LIKE '" + containerValue + "%' ";
      }
      filter += ")";
    } else {
      if (container.isExact)
        filter += "(`$.spec.containerId` = '" + containerValue + "')";
      else filter += "(`$.spec.containerId` LIKE '" + containerValue + "%')";
    }
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
  addressSpaceNameSpcae?: string,
  connectionName?: string
) => {
  let filter = "";
  if (addressSpaceName) {
    filter += "`$.spec.addressSpace` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpcae) {
    filter += "`$.metadata.namespace` = '" + addressSpaceNameSpcae + "' AND ";
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
  addressSpaceNameSpcae?: string,
  connectionName?: string,
  filterRole?: string
) => {
  let filter = "";
  let filterForLink = "";
  let filterNamesLength = filterNames && filterNames.length;
  let filterName = filterNames && filterNames[0];
  let filterNameValue =
    filterName && filterName.value && filterName.value.trim();

  let filterAddressesLength = filterAddresses && filterAddresses.length;
  let filterAddresse = filterAddresses && filterAddresses[0];
  let filterAddresseValue =
    filterAddresse && filterAddresse.value && filterAddresse.value.trim();

  if (addressSpaceName) {
    filter += "`$.spec.addressSpace` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpcae) {
    filter += "`$.metadata.namespace` = '" + addressSpaceNameSpcae + "' AND ";
  }
  if (connectionName) {
    filter += "`$.metadata.name` = '" + connectionName + "'";
  }
  //filter for links
  if (filterNamesLength > 0) {
    if (filterNamesLength > 1) {
      if (filterName.isExact)
        filterForLink += "(`$.metadata.name` = '" + filterNameValue + "'";
      else
        filterForLink += "(`$.metadata.name` LIKE '" + filterNameValue + "%' ";
      for (let i = 1; i < filterNamesLength; i++) {
        let filterName = filterNames && filterNames[i];
        let filterNameValue =
          filterName && filterName.value && filterName.value.trim();
        if (filterName.isExact)
          filterForLink += "OR `$.metadata.name` = '" + filterNameValue + "'";
        else
          filterForLink +=
            "OR `$.metadata.name` LIKE '" + filterNameValue + "%' ";
      }
      filterForLink += ")";
    } else {
      if (filterName.isExact)
        filterForLink += "`$.metadata.name` = '" + filterNameValue + "'";
      else
        filterForLink += "`$.metadata.name` LIKE '" + filterNameValue + "%' ";
    }
    if (filterAddressesLength > 0 || (filterRole && filterRole.trim() !== "")) {
      filterForLink += " AND ";
    }
  }
  if (filterAddressesLength > 0) {
    if (filterAddressesLength > 1) {
      if (filterAddresse.isExact)
        filterForLink += "(`$.spec.address` = '" + filterAddresseValue + "'";
      else
        filterForLink +=
          "(`$.spec.address` LIKE '" + filterAddresseValue + "%' ";
      for (let i = 1; i < filterAddresses.length; i++) {
        if (filterAddresse.isExact)
          filterForLink +=
            "OR `$.spec.address` = '" + filterAddresseValue + "'";
        else
          filterForLink +=
            "OR `$.spec.address` LIKE '" + filterAddresseValue + "%' ";
      }
      filterForLink += ")";
    } else {
      if (filterAddresse.isExact)
        filterForLink += "`$.spec.address` = '" + filterAddresseValue + "'";
      else
        filterForLink +=
          "`$.spec.Address` LIKE '" + filterAddresseValue + "%' ";
    }
    if (filterRole && filterRole.trim() !== "") {
      filterForLink += " AND ";
    }
  }

  if (filterRole && filterRole.trim() !== "") {
    filterForLink +=
      "`$.spec.role` = '" + filterRole.trim().toLowerCase() + "' ";
  }
  return { filter, filterForLink };
};

const RETURN_CONNECTION_LINKS = (
  page: number,
  perPage: number,
  filterNames: any[],
  filterAddresses: any[],
  addressSpaceName?: string,
  addressSpaceNameSpcae?: string,
  connectionName?: string,
  sortBy?: ISortBy,
  filterRole?: string
) => {
  const { filter, filterForLink } = CONNECTION_LINKS_FILTER(
    filterNames,
    filterAddresses,
    addressSpaceName,
    addressSpaceNameSpcae,
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
  isHostname: boolean,
  searchValue: string,
  name?: string,
  namespace?: string
) => {
  let filter = "";
  if (name) {
    filter += "`$.spec.addressSpace` = '" + name + "'";
  }
  if (namespace) {
    filter += " AND `$.metadata.namespace` = '" + namespace + "'";
  }
  if (searchValue.trim() !== "") {
    filter += " AND ";
    if (isHostname) {
      filter += "`$.spec.hostname` LIKE '" + searchValue.trim() + "%' ";
    } else {
      filter += "`$.spec.containerId` LIKE '" + searchValue.trim() + "%' ";
    }
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
