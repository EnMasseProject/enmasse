/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { ISortBy } from "@patternfly/react-table";

const ALL_CONECTION_LIST_FILTER=(
  hostnames: any[],
  containers: any[],
  name?: string,
  namespace?: string,
)=>{
  let filter="";
  let hostnamesLength=hostnames && hostnames.length;
  let hostname=hostnames && hostnames[0];
  let hostnameValue=hostname && hostname.value && hostname.value.trim();

  let containersLength=containers && containers.length;
  let container=containers && containers[0];
  let containerValue=container && container.value && container.value.trim();

  if (name) {
    filter += "`$.Spec.AddressSpace` = '" + name + "'";
  }
  if (namespace) {
    filter += " AND `$.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  if (
    (hostnamesLength > 0) ||
    (containers && containers.length > 0)
  ) {
    filter += " AND ";
  }
  if (hostnamesLength > 0) {
    if (hostnamesLength > 1) {
      if (hostname.isExact)
        filter += "(`$.Spec.Hostname` = '" + hostnameValue + "'";
      else
        filter +=
          "(`$.Spec.Hostname` LIKE '" + hostnameValue+ "%' ";
      for (let i = 1; i < hostnamesLength; i++) {
        let hostname=hostnames && hostnames[i];
        let hostnameValue=hostname && hostname.value && hostname.value.trim();
        if (hostname.isExact)
          filter +=
            "OR `$.Spec.Hostname` = '" + hostnameValue + "'";
        else
          filter +=
            "OR `$.Spec.Hostname` LIKE '" + hostnameValue + "%' ";
      }
      filter += ")";
    } else {
      if (hostname.isExact)
        filter += "(`$.Spec.Hostname` = '" + hostnameValue + "')";
      else
        filter +=
          "(`$.Spec.Hostname` LIKE '" + hostnameValue + "%')";
    }
  }

  if (containersLength> 0) {
    if (hostnamesLength > 0) {
      filter += " AND ";
    }
    if (containersLength> 1) {
      if (container.isExact)
        filter +=
          "(`$.Spec.ContainerId` = '" + containerValue + "'";
      else
        filter +=
          "(`$.Spec.ContainerId` LIKE '" + containerValue + "%' ";
      for (let i = 1; i < containersLength; i++) {
        let container=containers && containers[i];
        let containerValue=container && container.value && container.value.trim();
        if (container.isExact)
          filter +=
            "OR `$.Spec.ContainerId` = '" + containerValue + "'";
        else
          filter +=
            "OR `$.Spec.ContainerId` LIKE '" +
            containerValue +
            "%' ";
      }
      filter += ")";
    } else {
      if (container.isExact)
        filter +=
          "(`$.Spec.ContainerId` = '" + containerValue + "')";
      else
        filter +=
          "(`$.Spec.ContainerId` LIKE '" + containerValue + "%')";
    }
  }
  return filter;
};

const ALL_CONECTION_LIST_SORT=(sortBy?: ISortBy)=>{
  let orderBy="";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
        orderBy = "`$.Spec.Hostname` ";
        break;
      case 1:
        orderBy = "`$.Spec.ContainerId` ";
        break;
      case 2:
        orderBy = "`$.Spec.Protocol` ";
        break;
      case 3:
        orderBy = "`$.ObjectMeta.CreationTimestamp` ";
        break;
      case 4:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_messages_in')].Value` ";
        break;
      case 5:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_messages_out')].Value` ";
        break;
      case 6:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_senders')].Value` ";
        break;
      case 7:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_receivers')].Value` ";
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
  let filter =ALL_CONECTION_LIST_FILTER(hostnames,containers,name,namespace);
  let orderByString =ALL_CONECTION_LIST_SORT(sortBy);

  const ALL_CONECTION_LIST = gql(
    `query all_connections_for_addressspace_view {
        connections(
          filter: "${filter}" first:${perPage} offset:${perPage *
      (page - 1)} orderBy:"${orderByString}" 
        ) {
        Total
        Connections {
          ObjectMeta {
            Name
            CreationTimestamp
          }
          Spec {
            Hostname
            ContainerId
            Protocol
            Encrypted
          }
          Metrics {
            Name
            Type
            Value
            Units
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
    filter += "`$.Spec.AddressSpace` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpcae) {
    filter += "`$.ObjectMeta.Namespace` = '" + addressSpaceNameSpcae + "' AND ";
  }
  if (connectionName) {
    filter += "`$.ObjectMeta.Name` = '" + connectionName + "'";
  }
  const CONNECTION_DETAIL = gql`
    query single_connections {
      connections(
        filter: "${filter}" 
      ) {
        Total
        Connections {
          ObjectMeta {
            Name
            Namespace
            CreationTimestamp
          }
          Spec {
            Hostname
            ContainerId
            Protocol
            Encrypted
            Properties{
              Key
              Value
            }
          }
          Metrics {
            Name
            Type
            Value
            Units
          }
        }
      }
    }
    `;
  return CONNECTION_DETAIL;
};

const CONNECTION_LINKS_SORT=( sortBy?: ISortBy,)=>{
  let orderBy="";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
        orderBy = "";
        break;
      case 1:
        orderBy = "`$.ObjectMeta.Name` ";
        break;
      case 2:
        orderBy = "`$.Spec.Address` ";
        break;
      case 3:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_deliveries')].Value` ";
        break;
      case 4:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_accepted')].Value` ";
        break;
      case 5:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_rejected')].Value` ";
        break;
      case 5:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_released')].Value` ";
        break;
      case 7:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_modified')].Value` ";
        break;
      case 8:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_presettled')].Value` ";
        break;
      case 9:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_undelivered')].Value` ";
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

const CONNECTION_LINKS_FILTER=(
  filterNames: any[],
  filterAddresses: any[],
  addressSpaceName?: string,
  addressSpaceNameSpcae?: string,
  connectionName?: string,
  filterRole?: string
)=>{
  let filter=""; 
  let filterForLink="";
  let filterNamesLength=filterNames && filterNames.length;
  let filterName=filterNames && filterNames[0];
  let filterNameValue=filterName && filterName.value && filterName.value.trim();

  let filterAddressesLength=filterAddresses && filterAddresses.length;
  let filterAddresse=filterAddresses && filterAddresses[0];
  let filterAddresseValue=filterAddresse && filterAddresse.value && filterAddresse.value.trim();

  if (addressSpaceName) {
    filter += "`$.Spec.AddressSpace` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpcae) {
    filter += "`$.ObjectMeta.Namespace` = '" + addressSpaceNameSpcae + "' AND ";
  }
  if (connectionName) {
    filter += "`$.ObjectMeta.Name` = '" + connectionName + "'";
  }
  //filter for links
  if (filterNamesLength> 0) {
    if (filterNamesLength> 1) {
      if (filterName.isExact)
        filterForLink +=
          "(`$.ObjectMeta.Name` = '" + filterNameValue + "'";
      else
        filterForLink +=
          "(`$.ObjectMeta.Name` LIKE '" + filterNameValue + "%' ";
      for (let i = 1; i < filterNamesLength; i++) {
        let filterName=filterNames && filterNames[i];
        let filterNameValue=filterName && filterName.value && filterName.value.trim();
        if (filterName.isExact)
          filterForLink +=
            "OR `$.ObjectMeta.Name` = '" + filterNameValue + "'";
        else
          filterForLink +=
            "OR `$.ObjectMeta.Name` LIKE '" +
            filterNameValue +
            "%' ";
      }
      filterForLink += ")";
    } else {
      if (filterName.isExact)
        filterForLink +=
          "`$.ObjectMeta.Name` = '" + filterNameValue + "'";
      else
        filterForLink +=
          "`$.ObjectMeta.Name` LIKE '" + filterNameValue + "%' ";
    }
    if (
      (filterAddressesLength> 0) ||
      (filterRole && filterRole.trim() != "")
    ) {
      filterForLink += " AND ";
    }
  }
  if (filterAddressesLength> 0) {
    if (filterAddressesLength > 1) {
      if (filterAddresse.isExact)
        filterForLink +=
          "(`$.Spec.Address` = '" + filterAddresseValue+ "'";
      else
        filterForLink +=
          "(`$.Spec.Address` LIKE '" + filterAddresseValue + "%' ";
      for (let i = 1; i < filterAddresses.length; i++) {
        if (filterAddresse.isExact)
          filterForLink +=
            "OR `$.Spec.Address` = '" + filterAddresseValue + "'";
        else
          filterForLink +=
            "OR `$.Spec.Address` LIKE '" +
            filterAddresseValue +
            "%' ";
      }
      filterForLink += ")";
    } else {
      if (filterAddresse.isExact)
        filterForLink +=
          "`$.Spec.Address` = '" + filterAddresseValue + "'";
      else
        filterForLink +=
          "`$.Spec.Address` LIKE '" + filterAddresseValue + "%' ";
    }
    if (filterRole && filterRole.trim() != "") {
      filterForLink += " AND ";
    }
  }

  if (filterRole && filterRole.trim() != "") {
    filterForLink +=
      "`$.Spec.Role` = '" + filterRole.trim().toLowerCase() + "' ";
  }
  return {filter,filterForLink};
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
  const {filter,filterForLink}=CONNECTION_LINKS_FILTER(filterNames,filterAddresses,addressSpaceName,addressSpaceNameSpcae,connectionName,filterRole); 
  let orderBy =CONNECTION_LINKS_SORT(sortBy);
    
  const CONNECTION_DETAIL = gql`
    query single_connections {
      connections(
        filter: "${filter}" 
      ) {
        Total
        Connections {
          ObjectMeta {
            Name
            Namespace
          }
          Links(first:${perPage} offset:${perPage *
    (page - 1)} orderBy:"${orderBy}"
      filter:"${filterForLink}") {
            Total
            Links {
              ObjectMeta {
                Name
              }
              Spec {
                Role
                Address
              }
              Metrics {
                Name
                Type
                Value
                Units
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
    filter += "`$.ObjectMeta.Namespace` = '" + namespace + "' AND ";
  }
  if (connectionName) {
    filter += "`$.ObjectMeta.Name` = '" + connectionName + "'";
  }
  const all_links = gql`
    query single_connections {
      connections(
        filter: "${filter}" 
      ) {
        Total
        Connections {
          Links(first:10 offset:0 filter:"\`$.ObjectMeta.Name\` LIKE '${name}%'") {
            Total
            Links {
              ObjectMeta {
                Name
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
    filter += "`$.ObjectMeta.Namespace` = '" + namespace + "' AND ";
  }
  if (connectionName) {
    filter += "`$.ObjectMeta.Name` = '" + connectionName + "'";
  }
  const all_links = gql`
    query single_connections {
      connections(
        filter: "${filter}" 
      ) {
        Total
        Connections {
          Links(first:10 offset:0
                filter:"\`$.Spec.Address\` LIKE '${address}%'") {
            Links {
              Spec {
                Address
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
    filter += "`$.Spec.AddressSpace` = '" + name + "'";
  }
  if (namespace) {
    filter += " AND `$.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  if (searchValue.trim() != "") {
    filter += " AND ";
    if (isHostname) {
      filter += "`$.Spec.Hostname` LIKE '" + searchValue.trim() + "%' ";
    } else {
      filter += "`$.Spec.ContainerId` LIKE '" + searchValue.trim() + "%' ";
    }
  }

  const ALL_CONECTION_LIST = gql(
    `query all_connections_for_addressspace_view {
        connections(
          filter: "${filter}" first:10 offset:0
        ) {
        Total
        Connections {
          Spec {
            Hostname
            ContainerId
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
