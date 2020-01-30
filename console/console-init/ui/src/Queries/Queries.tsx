/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { ISortBy } from "@patternfly/react-table";

export const DOWNLOAD_CERTIFICATE = gql`
  query messagingCertificateChain($as: ObjectMeta_v1_Input!) {
    messagingCertificateChain(input: $as)
  }
`;
export const DELETE_ADDRESS_SPACE = gql`
  mutation delete_as($a: ObjectMeta_v1_Input!) {
    deleteAddressSpace(input: $a)
  }
`;
export const DELETE_ADDRESS = gql`
  mutation delete_addr($a: ObjectMeta_v1_Input!) {
    deleteAddress(input: $a)
  }
`;
export const PURGE_ADDRESS = gql`
  mutation purge_addr($a: ObjectMeta_v1_Input!) {
    purgeAddress(input: $a)
  }
`;

export const RETURN_ALL_ADDRESS_SPACES = (
  page: number,
  perPage: number,
  filter_Names?: any[],
  filter_NameSpace?: any[],
  filter_Type?: string | null,
  sortBy?: ISortBy
) => {
  let filter = "";
  if (filter_Names && filter_Names.length > 0) {
    if (filter_Names.length > 1) {
      if (filter_Names[0].isExact)
        filter +=
          "(`$.objectMeta.name` = '" + filter_Names[0].value.trim() + "'";
      else
        filter +=
          "(`$.objectMeta.name` LIKE '" + filter_Names[0].value.trim() + "%'";
      for (let i = 1; i < filter_Names.length; i++) {
        if (filter_Names[i].isExact)
          filter +=
            "OR `$.objectMeta.name` = '" + filter_Names[i].value.trim() + "'";
        else
          filter +=
            "OR `$.objectMeta.name` LIKE '" +
            filter_Names[i].value.trim() +
            "%'";
      }
      filter += ")";
    } else {
      if (filter_Names[0].isExact)
        filter +=
          "`$.objectMeta.name` = '" + filter_Names[0].value.trim() + "'";
      else
        filter +=
          "`$.objectMeta.name` LIKE '" + filter_Names[0].value.trim() + "%'";
    }
  }
  if (
    filter_Names &&
    filter_Names.length > 0 &&
    filter_NameSpace &&
    filter_NameSpace.length > 0
  ) {
    filter += " AND ";
  }
  if (filter_NameSpace && filter_NameSpace.length > 0) {
    if (filter_NameSpace.length > 1) {
      if (filter_NameSpace[0].isExact)
        filter +=
          "(`$.objectMeta.namespace` = '" +
          filter_NameSpace[0].value.trim() +
          "'";
      else
        filter +=
          "(`$.objectMeta.namespace` LIKE '" +
          filter_NameSpace[0].value.trim() +
          "%'";
      for (let i = 1; i < filter_NameSpace.length; i++) {
        if (filter_NameSpace[i].isExact)
          filter +=
            "OR `$.objectMeta.namespace` = '" +
            filter_NameSpace[i].value.trim() +
            "'";
        else
          filter +=
            "OR `$.objectMeta.namespace` LIKE '" +
            filter_NameSpace[i].value.trim() +
            "%'";
      }
      filter += ")";
    } else {
      if (filter_NameSpace[0].isExact)
        filter +=
          "`$.objectMeta.namespace` = '" +
          filter_NameSpace[0].value.trim() +
          "'";
      else
        filter +=
          "`$.objectMeta.namespace` LIKE '" +
          filter_NameSpace[0].value.trim() +
          "%'";
    }
  }
  if (
    ((filter_Names && filter_Names.length > 0) ||
      (filter_NameSpace && filter_NameSpace.length > 0)) &&
    filter_Type &&
    filter_Type.trim() !== ""
  ) {
    filter += " AND ";
  }
  if (filter_Type && filter_Type.trim() !== "") {
    filter += "`$.spec.type` ='" + filter_Type.toLowerCase().trim() + "' ";
  }
  let orderByString = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 1:
        orderByString = "`$.objectMeta.name` ";
        break;
      case 2:
        break;
      case 3:
        break;
      case 4:
        orderByString = "`$.objectMeta.creationTimestamp` ";
        break;
      default:
        break;
    }
    if (sortBy.direction) {
      orderByString += sortBy.direction;
    }
  }
  const ALL_ADDRESS_SPACES = gql`
    query all_address_spaces {
      addressSpaces(filter: "${filter}"  
      first:${perPage} offset:${perPage *
    (page - 1)} orderBy:"${orderByString}") {
        Total
        AddressSpaces {
          objectMeta {
            namespace
            name
            creationTimestamp
          }
          spec {
            type
            plan {
              objectMeta{
                name
              }
              spec {
                displayName
              }
            }
          }
          status {
            isReady
            phase
            messages
          }
        }
      }
    }
  `;
  return ALL_ADDRESS_SPACES;
};

export const RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE = (
  page: number,
  perPage: number,
  name?: string,
  namespace?: string,
  filterNames?: any[],
  typeValue?: string | null,
  statusValue?: string | null,
  sortBy?: ISortBy
) => {
  let filterString = "";
  if (name && name.trim() !== "") {
    filterString += "`$.objectMeta.name` LIKE '" + name + ".%' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filterString += "`$.objectMeta.namespace` = '" + namespace + "'";
  }
  if ((filterNames && filterNames.length > 0) || typeValue || statusValue) {
    filterString += " AND ";
  }
  if (filterNames && filterNames.length > 0) {
    if (filterNames.length > 1) {
      if (filterNames[0].isExact)
        filterString +=
          "(`$.spec.address` = '" + filterNames[0].value.trim() + "'";
      else
        filterString +=
          "(`$.spec.address` LIKE '" + filterNames[0].value.trim() + "%' ";
      for (let i = 1; i < filterNames.length; i++) {
        if (filterNames[i].isExact) {
          filterString +=
            "OR `$.spec.address` = '" + filterNames[i].value.trim() + "'";
        } else {
          filterString +=
            "OR `$.spec.address` LIKE '" + filterNames[i].value.trim() + "%' ";
        }
      }
      filterString += ")";
    } else {
      if (filterNames[0].isExact)
        filterString +=
          "`$.spec.address` = '" + filterNames[0].value.trim() + "'";
      else
        filterString +=
          "`$.spec.address` LIKE '" + filterNames[0].value.trim() + "%' ";
    }
  }
  if (filterNames && filterNames.length > 0 && (typeValue || statusValue)) {
    filterString += " AND ";
  }
  if (typeValue) {
    filterString += "`$.spec.type` = '" + typeValue.toLowerCase() + "'";
  }
  if (typeValue && statusValue) {
    filterString += " AND ";
  }
  if (statusValue) {
    let status = "";
    if (statusValue === "Failed") {
      status = "Pending";
    } else {
      status = statusValue;
    }
    filterString += "`$.status.phase` = '" + status + "'";
  }
  let orderByString = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
        break;
      case 1:
        orderByString = "`$objectMeta.name` ";
        break;
      case 2:
        break;
      case 3:
        break;
      case 4:
        orderByString = "`$.metrics[?(@.Name=='enmasse_messages_in')].Value` ";
        break;
      case 5:
        orderByString = "`$.metrics[?(@.Name=='enmasse_messages_out')].Value` ";
        break;
      case 6:
        orderByString =
          "`$.metrics[?(@.Name=='enmasse_messages_stored')].Value` ";
        break;
      case 7:
        orderByString = "`$.metrics[?(@.Name=='enmasse_senders')].Value` ";
        break;
      case 8:
        orderByString = "`$.metrics[?(@.Name=='enmasse_receivers')].Value` ";
        break;
    }
    if (orderByString !== "" && sortBy.direction) {
      orderByString += sortBy.direction;
    }
  }
  const ALL_ADDRESS_FOR_ADDRESS_SPACE = gql`
  query all_addresses_for_addressspace_view {
    addresses( first:${perPage} offset:${perPage * (page - 1)}
      filter:"${filterString}"
      orderBy:"${orderByString}"
    ) {
      Total
      Addresses {
        objectMeta {
          namespace
          name
        }
        spec {
          address
          type
          plan {
            spec {
              displayName
            }
            objectMeta {
              name
            }
          }
        }
        status {
          planStatus{
            partitions
          }
          phase
          isReady
          messages
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
  return ALL_ADDRESS_FOR_ADDRESS_SPACE;
};

export const RETURN_ADDRESS_SPACE_DETAIL = (
  name?: string,
  namespace?: string
) => {
  const ADDRESS_SPACE_DETAIL = gql`
    query all_address_spaces {
      addressSpaces(
        filter: "\`$..name\` = '${name}' AND \`$..namespace\` = '${namespace}'"
      ) {
        AddressSpaces {
          objectMeta {
            namespace
            name
            creationTimestamp
          }
          spec {
            type
            plan {
              objectMeta {
                name
              }
              spec {
                displayName
              }   
            }
          }
          status {
            isReady
            messages
          }
        }
      }
    }`;
  return ADDRESS_SPACE_DETAIL;
};

export const CURRENT_ADDRESS_SPACE_PLAN = (
  name?: string,
  namespace?: string
) => {
  const ADDRESS_SPACE_PLAN = gql`
    query all_address_spaces {
      addressSpaces(
        filter: "\`$..name\` = '${name}' AND \`$..namespace\` = '${namespace}'"
      ) {
        AddressSpaces {
          spec {
            plan {
              objectMeta {
                name
              }   
            }
          }
        }
      }
    }`;
  return ADDRESS_SPACE_PLAN;
};

export const RETURN_ADDRESS_DETAIL = (
  addressSpace?: string,
  namespace?: string,
  addressName?: string
) => {
  let filter = "";
  if (addressSpace) {
    filter += "`$.objectMeta.name` LIKE '" + addressSpace + ".%' AND ";
  }
  if (namespace) {
    filter += "`$.objectMeta.namespace` = '" + namespace + "' AND ";
  }
  if (addressName) {
    filter += "`$.objectMeta.name` = '" + addressName + "'";
  }
  const ADDRESSDETAIL = gql`
  query single_addresses {
    addresses(
      filter: "${filter}" 
    ) {
      Total
      Addresses {
        objectMeta {
          namespace
          name
          creationTimestamp
        }
        spec {
          address
          topic
          plan {
            spec {
              displayName
              addressType
            }
          }
        }
        status {
          isReady
          messages
          phase
          planStatus {
            partitions
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
  return ADDRESSDETAIL;
};

export const RETURN_ADDRESS_LINKS = (
  page: number,
  perPage: number,
  filterNames: any[],
  filterContainers: any[],
  addressSpace?: string,
  namespace?: string,
  addressName?: string,
  sortBy?: ISortBy,
  filterRole?: string
) => {
  let filter = "";
  if (addressSpace) {
    filter += "`$.objectMeta.name` LIKE '" + addressSpace + ".%' AND ";
  }
  if (namespace) {
    filter += "`$.objectMeta.namespace` = '" + namespace + "' AND ";
  }
  if (addressName) {
    filter += "`$.objectMeta.name` = '" + addressName + "'";
  }
  let orderByString = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
        orderByString = "";
        break;
      case 1:
        orderByString = "";
        break;
      case 2:
        orderByString = "`$.objectMeta.name` ";
        break;
      case 3:
        orderByString = "`$.metrics[?(@.name=='enmasse_messages_in')].Value` ";
        break;
      case 4:
        orderByString =
          "`$.netrics[?(@.name=='enmasse_messages_backlog')].Value` ";
        break;
    }
    orderByString += sortBy.direction;
  }

  let filterForLink = "";
  if (filterNames && filterNames.length > 0) {
    if (filterNames.length > 1) {
      if (filterNames[0].isExact)
        filterForLink +=
          "(`$.objectMeta.name` = '" + filterNames[0].value.trim() + "'";
      else
        filterForLink +=
          "(`$.objectMeta.name` LIKE '" + filterNames[0].value.trim() + "%' ";
      for (let i = 1; i < filterNames.length; i++) {
        if (filterNames[i].isExact)
          filterForLink +=
            "OR `$.objectMeta.name` = '" + filterNames[i].value.trim() + "'";
        else
          filterForLink +=
            "OR `$.objectMeta.name` LIKE '" +
            filterNames[i].value.trim() +
            "%' ";
      }
      filterForLink += ")";
    } else {
      if (filterNames[0].isExact)
        filterForLink +=
          "(`$.objectMeta.name` = '" + filterNames[0].value.trim() + "')";
      else
        filterForLink +=
          "(`$.objectMeta.name` LIKE '" + filterNames[0].value.trim() + "%')";
    }
    if (
      (filterContainers && filterContainers.length > 0) ||
      (filterRole && filterRole.trim() != "")
    ) {
      filterForLink += " AND ";
    }
  }
  if (filterContainers && filterContainers.length > 0) {
    if (filterContainers.length > 1) {
      if (filterContainers[0].isExact)
        filterForLink +=
          "(`$.spec.connection.spec.containerId` = '" +
          filterContainers[0].value.trim() +
          "'";
      else
        filterForLink +=
          "(`$.spec.connection.spec.containerId` LIKE '" +
          filterContainers[0].value.trim() +
          "%'";
      for (let i = 1; i < filterContainers.length; i++) {
        if (filterContainers[i].isExact)
          filterForLink +=
            "OR `$.spec.connection.spec.containerId` = '" +
            filterContainers[i].value.trim() +
            "'";
        else
          filterForLink +=
            "OR `$.spec.connection.spec.containerId` LIKE '" +
            filterContainers[i].value.trim() +
            "%";
      }
      filterForLink += ")";
    } else {
      if (filterContainers[0].isExact)
        filterForLink +=
          "(`$.spec.connection.spec.containerId` = '" +
          filterContainers[0].value.trim() +
          "')";
      else
        filterForLink +=
          "(`$.spec.connection.spec.containerId` LIKE '" +
          filterContainers[0].value.trim() +
          "%')";
    }
    if (filterRole && filterRole.trim() != "") {
      filterForLink += " AND ";
    }
  }

  if (filterRole && filterRole.trim() != "") {
    filterForLink +=
      "`$.spec.role` = '" + filterRole.trim().toLowerCase() + "' ";
  }

  const query = gql`
  query single_address_with_links_and_metrics {
    addresses(
      filter: "${filter}"
    ) {
      Total
      Addresses {
        objectMeta {
          name
        }
        spec {
          addressSpace
        }
        links (first:${perPage} offset:${perPage *
    (page - 1)}  orderBy:"${orderByString}" filter:"${filterForLink}"){
          total
          Links {
            objectMeta {
              name
            }
            spec {
              role
              connection {
                objectMeta {
                  name
                  namespace
                }
                spec {
                  containerId
                }
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
    }
  }
  `;
  return query;
};

export const RETURN_ADDRESS_PLANS = (
  addressSpacePlan: string,
  addressType: string
) => {
  const ADDRESS_PLANS = gql`
  query all_address_plans {
    addressPlans(addressSpacePlan: "${addressSpacePlan}", addressType: ${addressType}) {
      objectMeta {
        name
      }
      spec {
        addressType
        displayName
        longDescription
        shortDescription
      }
    }
  }
`;
  return ADDRESS_PLANS;
};

export const CREATE_ADDRESS = gql`
  mutation create_addr($a: Address_enmasse_io_v1beta1_Input!) {
    createAddress(input: $a) {
      name
      namespace
      uid
    }
  }
`;

export const CREATE_ADDRESS_SPACE = gql`
  mutation create_as($as: AddressSpace_enmasse_io_v1beta1_Input!) {
    createAddressSpace(input: $as) {
      name
      uid
      creationTimestamp
    }
  }
`;

export const EDIT_ADDRESS_SPACE = gql`
  mutation patch_as(
    $a: ObjectMeta_v1_Input!
    $jsonPatch: String!
    $patchType: String!
  ) {
    patchAddressSpace(input: $a, jsonPatch: $jsonPatch, patchType: $patchType)
  }
`;

export const EDIT_ADDRESS = gql`
  mutation patch_addr(
    $a: ObjectMeta_v1_Input!
    $jsonPatch: String!
    $patchType: String!
  ) {
    patchAddress(input: $a, jsonPatch: $jsonPatch, patchType: $patchType)
  }
`;

export const ADDRESS_COMMAND_PRIVIEW_DETAIL = gql`
  query cmd($a: Address_enmasse_io_v1beta1_Input!) {
    addressCommand(input: $a)
  }
`;

export const ADDRESS_SPACE_COMMAND_REVIEW_DETAIL = gql`
  query cmd($as: AddressSpace_enmasse_io_v1beta1_Input!) {
    addressSpaceCommand(input: $as)
  }
`;

export const RETURN_ALL_CONECTION_LIST = (
  page: number,
  perPage: number,
  hostnames: any[],
  containers: any[],
  name?: string,
  namespace?: string,
  sortBy?: ISortBy
) => {
  let filter = "";
  if (name) {
    filter += "`$.spec.addressSpace` = '" + name + "'";
  }
  if (namespace) {
    filter += " AND `$.objectMeta.namespace` = '" + namespace + "'";
  }
  if (
    (hostnames && hostnames.length > 0) ||
    (containers && containers.length > 0)
  ) {
    filter += " AND ";
  }
  if (hostnames && hostnames.length > 0) {
    if (hostnames.length > 1) {
      if (hostnames[0].isExact)
        filter += "(`$.spec.hostname` = '" + hostnames[0].value.trim() + "'";
      else
        filter +=
          "(`$.spec.hostname` LIKE '" + hostnames[0].value.trim() + "%' ";
      for (let i = 1; i < hostnames.length; i++) {
        if (hostnames[i].isExact)
          filter +=
            "OR `$.spec.hostname` = '" + hostnames[i].value.trim() + "'";
        else
          filter +=
            "OR `$.spec.hostname` LIKE '" + hostnames[i].value.trim() + "%' ";
      }
      filter += ")";
    } else {
      if (hostnames[0].isExact)
        filter += "(`$.spec.hostname` = '" + hostnames[0].value.trim() + "')";
      else
        filter +=
          "(`$.spec.hostname` LIKE '" + hostnames[0].value.trim() + "%')";
    }
  }

  if (containers && containers.length > 0) {
    if (hostnames && hostnames.length > 0) {
      filter += " AND ";
    }
    if (containers.length > 1) {
      if (containers[0].isExact)
        filter +=
          "(`$.spec.containerId` = '" + containers[0].value.trim() + "'";
      else
        filter +=
          "(`$.spec.containerId` LIKE '" + containers[0].value.trim() + "%' ";
      for (let i = 1; i < containers.length; i++) {
        if (containers[i].isExact)
          filter +=
            "OR `$.spec.containerId` = '" + containers[i].value.trim() + "'";
        else
          filter +=
            "OR `$.spec.containerId` LIKE '" +
            containers[i].value.trim() +
            "%' ";
      }
      filter += ")";
    } else {
      if (containers[0].isExact)
        filter +=
          "(`$.spec.containerId` = '" + containers[0].value.trim() + "')";
      else
        filter +=
          "(`$.spec.containerId` LIKE '" + containers[0].value.trim() + "%')";
    }
  }

  let orderByString = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
        orderByString = "`$.spec.hostname` ";
        break;
      case 1:
        orderByString = "`$.spec.containerId` ";
        break;
      case 2:
        orderByString = "`$.spec.protocol` ";
        break;
      case 3:
        orderByString = "`$.objectMeta.creationTimestamp` ";
        break;
      case 4:
        orderByString = "`$.metrics[?(@.name=='enmasse_messages_in')].Value` ";
        break;
      case 5:
        orderByString = "`$.metrics[?(@.name=='enmasse_messages_out')].Value` ";
        break;
      case 6:
        orderByString = "`$.metrics[?(@.name=='enmasse_senders')].Value` ";
        break
      case 7:
        orderByString = "`$.metrics[?(@.name=='enmasse_receivers')].Value` ";
        break;
    }
    orderByString += sortBy.direction;
  }

  const ALL_CONECTION_LIST = gql(
    `query all_connections_for_addressspace_view {
      connections(
        filter: "${filter}" first:${perPage} offset:${perPage *
      (page - 1)} orderBy:"${orderByString}" 
      ) {
      Total
      Connections {
        objectMeta {
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

export const RETURN_CONNECTION_DETAIL = (
  addressSpaceName?: string,
  addressSpaceNameSpcae?: string,
  connectionName?: string
) => {
  let filter = "";
  if (addressSpaceName) {
    filter += "`$.spec.addressSpace` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpcae) {
    filter += "`$.objectMeta.namespace` = '" + addressSpaceNameSpcae + "' AND ";
  }
  if (connectionName) {
    filter += "`$.objectMeta.name` = '" + connectionName + "'";
  }
  const CONNECTION_DETAIL = gql`
  query single_connections {
    connections(
      filter: "${filter}" 
    ) {
      Total
      Connections {
        objectMeta {
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

export const RETURN_ADDRESS_TYPES = gql`
  query addressTypes($a: AddressSpaceType!) {
    addressTypes_v2(addressSpaceType: $a) {
      objectMeta {
        name
      }
      spec {
        displayName
        longDescription
        shortDescription
      }
    }
  }
`;
export const RETURN_NAMESPACES = gql`
  query all_namespaces {
    namespaces {
      objectMeta {
        name
      }
      status {
        phase
      }
    }
  }
`;

export const RETURN_ADDRESS_SPACE_PLANS = gql`
  query all_address_space_plans {
    addressSpacePlans {
      objectMeta {
        name
        uid
        creationTimestamp
      }
      spec {
        addressSpaceType
      }
    }
  }
`;

export const RETURN_CONNECTION_LINKS = (
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
  let filter = "";
  if (addressSpaceName) {
    filter += "`$.spec.addressSpace` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpcae) {
    filter += "`$.objectMeta.namespace` = '" + addressSpaceNameSpcae + "' AND ";
  }
  if (connectionName) {
    filter += "`$.objectMeta.name` = '" + connectionName + "'";
  }
  let orderByString = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
        orderByString = "";
        break;
      case 1:
        orderByString = "`$.objectMeta.name` ";
        break;
      case 2:
        orderByString = "`$.spec.address` ";
        break;
      case 3:
        orderByString = "`$.metrics[?(@.name=='enmasse_deliveries')].Value` ";
        break;
      case 4:
        orderByString = "`$.metrics[?(@.name=='enmasse_accepted')].value` ";
        break;
      case 5:
        orderByString = "`$.metrics[?(@.name=='enmasse_rejected')].Value` ";
        break;
      case 5:
        orderByString = "`$.metrics[?(@.name=='enmasse_released')].Value` ";
        break;
      case 7:
        orderByString = "`$.metrics[?(@.name=='enmasse_modified')].Value` ";
        break;
      case 8:
        orderByString = "`$.metrics[?(@.name=='enmasse_presettled')].Value` ";
        break;
      case 9:
        orderByString = "`$.metrics[?(@.name=='enmasse_undelivered')].Value` ";
        break;
    }
    if (sortBy.direction && orderByString !== "") {
      orderByString += sortBy.direction;
    }
  }
  let filterForLink = "";
  if (filterNames && filterNames.length > 0) {
    if (filterNames.length > 1) {
      if (filterNames[0].isExact)
        filterForLink +=
          "(`$.objectMeta.name` = '" + filterNames[0].value.trim() + "'";
      else
        filterForLink +=
          "(`$.objectMeta.name` LIKE '" + filterNames[0].value.trim() + "%' ";
      for (let i = 1; i < filterNames.length; i++) {
        if (filterNames[i].isExact)
          filterForLink +=
            "OR `$.objectMeta.name` = '" + filterNames[i].value.trim() + "'";
        else
          filterForLink +=
            "OR `$.objectMeta.name` LIKE '" +
            filterNames[i].value.trim() +
            "%' ";
      }
      filterForLink += ")";
    } else {
      if (filterNames[0].isExact)
        filterForLink +=
          "`$.objectMeta.name` = '" + filterNames[0].value.trim() + "'";
      else
        filterForLink +=
          "`$.objectMeta.name` LIKE '" + filterNames[0].value.trim() + "%' ";
    }
    if (
      (filterAddresses && filterAddresses.length > 0) ||
      (filterRole && filterRole.trim() != "")
    ) {
      filterForLink += " AND ";
    }
  }
  if (filterAddresses && filterAddresses.length > 0) {
    if (filterAddresses.length > 1) {
      if (filterAddresses[0].isExact)
        filterForLink +=
          "(`$.spec.address` = '" + filterAddresses[0].value.trim() + "'";
      else
        filterForLink +=
          "(`$.spec.address` LIKE '" + filterAddresses[0].value.trim() + "%' ";
      for (let i = 1; i < filterAddresses.length; i++) {
        if (filterAddresses[i].isExact)
          filterForLink +=
            "OR `$.spec.address` = '" + filterAddresses[i].value.trim() + "'";
        else
          filterForLink +=
            "OR `$.spec.address` LIKE '" +
            filterAddresses[i].value.trim() +
            "%' ";
      }
      filterForLink += ")";
    } else {
      if (filterAddresses[0].isExact)
        filterForLink +=
          "`$.spec.address` = '" + filterAddresses[0].value.trim() + "'";
      else
        filterForLink +=
          "`$.spec.address` LIKE '" + filterAddresses[0].value.trim() + "%' ";
    }
    if (filterRole && filterRole.trim() != "") {
      filterForLink += " AND ";
    }
  }

  if (filterRole && filterRole.trim() != "") {
    filterForLink +=
      "`$.spec.role` = '" + filterRole.trim().toLowerCase() + "' ";
  }

  const CONNECTION_DETAIL = gql`
  query single_connections {
    connections(
      filter: "${filter}" 
    ) {
      Total
      Connections {
        objectMeta {
          name
          namespace
        }
        links(first:${perPage} offset:${perPage *
    (page - 1)} orderBy:"${orderByString}"
    filter:"${filterForLink}") {
          Total
          Links {
            objectMeta {
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

export const RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION = (
  name: string,
  namespace: string,
  type: string
) => {
  let filterString = "";
  if (name && name.trim() !== "") {
    filterString += "`$.objectMeta.name` LIKE '" + name + ".%' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filterString += "`$.objectMeta.namespace` = '" + namespace + "'";
  }
  if (type.trim().toLowerCase() === "subscription") {
    filterString += " AND `$.spec.type` = 'topic'";
  }
  const ALL_TOPICS_FOR_ADDRESS_SPACE = gql`
  query all_addresses_for_addressspace_view {
    addresses(
      filter:"${filterString}"
    ) {
      Total
      Addresses {
        objectMeta {
          namespace
          name
        }
        spec {
          address
          type
          plan {
            spec {
              displayName
            }
          }
        }
      }
    }
  }
`;
  return ALL_TOPICS_FOR_ADDRESS_SPACE;
};

export const RETURN_ALL_NAMES_OF_ADDRESS_LINK_FOR_TYPEAHEAD_SEARCH = (
  addressname: string,
  namespace: string,
  name: string
) => {
  const all_names = gql`
  query all_link_names_for_connection {
    addresses(
      filter: "\`$.objectMeta.name\` = '${addressname}' AND \`$.objectMeta.namespace\` = '${namespace}'"
    ) {
      Total
      Addresses {
        links(filter:"\`$.objectMeta.name\` LIKE '${name}%'" first:10,offset:0)  {
          Total
          Links{
            objectMeta {
              name
            }
          }
        }
      }
    }
  }
  `;
  return all_names;
};

export const RETURN_ALL_CONTAINER_IDS_OF_ADDRESS_LINKS_FOR_TYPEAHEAD_SEARCH = (
  addressname: string,
  namespace: string,
  container: string
) => {
  const all_names = gql`
  query all_link_names_for_connection {
    addresses(
      filter: "\`$.objectMeta.name\` = '${addressname}' AND \`$.objectMeta.namespace\` = '${namespace}'"
    ) {
      Total
      Addresses {
        links(filter:"\`$.spec.connection.spec.containerId\` LIKE '${container}%'" first:10 ,offset:0)  {
          Total
          Links{
            spec{
              connection{
                spec{
                  containerId
                }
              }
            }
          }
        }
      }
    }
  }
  `;
  return all_names;
};

export const RETURN_ALL_CONNECTION_LINKS_FOR_NAME_SEARCH = (
  connectionName: string,
  namespace: string,
  name: string
) => {
  let filter = "";
  if (namespace) {
    filter += "`$.objectMeta.namespace` = '" + namespace + "' AND ";
  }
  if (connectionName) {
    filter += "`$.objectMeta.name` = '" + connectionName + "'";
  }
  const all_links = gql`
  query single_connections {
    connections(
      filter: "${filter}" 
    ) {
      Total
      Connections {
        links(first:10 offset:0 filter:"\`$.objectMeta.name\` LIKE '${name}%'") {
          Total
          Links {
            objectMeta {
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

export const RETURN_ALL_CONNECTION_LINKS_FOR_ADDRESS_SEARCH = (
  connectionName: string,
  namespace: string,
  address: string
) => {
  let filter = "";
  if (namespace) {
    filter += "`$.objectMeta.namespace` = '" + namespace + "' AND ";
  }
  if (connectionName) {
    filter += "`$.objectMeta.name` = '" + connectionName + "'";
  }
  const all_links = gql`
  query single_connections {
    connections(
      filter: "${filter}" 
    ) {
      Total
      Connections {
        links(first:10 offset:0
              filter:"\`$.spec.address\` LIKE '${address}%'") {
          Links {
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

export const RETURN_ALL_ADDRESS_SPACES_FOR_NAME_OR_NAMESPACE = (
  isName: boolean,
  value: string
) => {
  let filter = "";
  if (value) {
    if (isName) {
      filter += "`$.objectMeta.name` LIKE '" + value + "%'";
    } else {
      filter += "`$.objectMeta.namespace` LIKE '" + value + "%'";
    }
  }
  const all_address_spaces = gql`
  query all_address_spaces {
    addressSpaces(filter: "${filter}"  
    first:100 offset:0) {
      Total
      AddressSpaces {
        objectMeta {
          namespace
          name
        }
      }
    }
  }
  `;
  return all_address_spaces;
};

export const RETURN_ALL_ADDRESS_NAMES_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH = (
  addressspaceName?: string,
  namespace?: string,
  name?: string
) => {
  let filterString = "";
  if (addressspaceName && addressspaceName.trim() !== "") {
    filterString += "`$.spec.addressSpace` = '" + addressspaceName + "' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filterString += "`$.objectMeta.namespace` = '" + namespace + "'";
  }
  if (name && name.trim() != "") {
    filterString += " AND ";
    filterString += "`$.spec.address` LIKE '" + name.trim() + "%' ";
  }
  const ALL_ADDRESS_FOR_ADDRESS_SPACE = gql`
  query all_addresses_for_addressspace_view {
    addresses( first:10 offset:0
      filter:"${filterString}"
    ) {
      Total
      Addresses {
        spec{
          address
        }
      }
    }
  }
`;
  return ALL_ADDRESS_FOR_ADDRESS_SPACE;
};

export const RETURN_ALL_CONNECTIONS_HOSTNAME_AND_CONTAINERID_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH = (
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
    filter += " AND `$.objectMeta.namespace` = '" + namespace + "'";
  }
  if (searchValue.trim() != "") {
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
      Total
      Connections {
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

export const RETURN_AUTHENTICATION_SERVICES = gql`
  query addressspace_schema {
    addressSpaceSchema_v2 {
      objectMeta {
        name
      }
      spec {
        authenticationServices
      }
    }
  }
`;

export const RETURN_WHOAMI = gql`
  query whoami {
    whoami {
      objectMeta {
        name
      }
      fullName
    }
  }
`;
