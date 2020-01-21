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
      if(filter_Names[0].isExact)
        filter += "(`$.ObjectMeta.Name` = '" + filter_Names[0].value.trim() + "'";
      else
        filter += "(`$.ObjectMeta.Name` LIKE '" + filter_Names[0].value.trim() + "%'";
      for (let i = 1; i < filter_Names.length; i++) {
        if(filter_Names[i].isExact)
          filter += "OR `$.ObjectMeta.Name` = '" + filter_Names[i].value.trim() + "'";
        else
          filter += "OR `$.ObjectMeta.Name` LIKE '" + filter_Names[i].value.trim() + "%'";
      }
      filter += ")";
    } else {
        if(filter_Names[0].isExact)
          filter += "`$.ObjectMeta.Name` = '" + filter_Names[0].value.trim() + "'";
        else
          filter += "`$.ObjectMeta.Name` LIKE '" + filter_Names[0].value.trim() + "%'";
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
          "(`$.ObjectMeta.Namespace` = '" + filter_NameSpace[0].value.trim() + "'";
      else
        filter +=
          "(`$.ObjectMeta.Namespace` LIKE '" + filter_NameSpace[0].value.trim() + "%'";
      for (let i = 1; i < filter_NameSpace.length; i++) {
        if (filter_NameSpace[i].isExact)
          filter +=
            "OR `$.ObjectMeta.Namespace` = '" + filter_NameSpace[i].value.trim() + "'";
        else
        filter +=
          "OR `$.ObjectMeta.Namespace` LIKE '" + filter_NameSpace[i].value.trim() + "%'";
      }
      filter += ")";
    } else {
      if (filter_NameSpace[0].isExact)
        filter +=
          "`$.ObjectMeta.Namespace` = '" + filter_NameSpace[0].value.trim() + "'";
      else
        filter +=
          "`$.ObjectMeta.Namespace` LIKE '" + filter_NameSpace[0].value.trim() + "%'";
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
    filter += "`$.Spec.Type` ='" + filter_Type.toLowerCase().trim() + "' ";
  }
  let orderByString = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 1:
        orderByString = "`$.ObjectMeta.Name` ";
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
          ObjectMeta {
            Namespace
            Name
            CreationTimestamp
          }
          Spec {
            Type
            Plan {
              Spec {
                DisplayName
              }
            }
          }
          Status {
            IsReady
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
    filterString += "`$.ObjectMeta.Name` LIKE '" + name + ".%' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filterString += "`$.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  if ((filterNames && filterNames.length > 0) || typeValue || statusValue) {
    filterString += " AND ";
  }
  if (filterNames && filterNames.length > 0) {
    if (filterNames.length > 1) {
      if(filterNames[0].isExact)
        filterString += "(`$.ObjectMeta.Name` = '" + filterNames[0].value.trim() + "'";
      else
        filterString += "(`$.ObjectMeta.Name` LIKE '" + filterNames[0].value.trim() + "%' ";
      for (let i = 1; i < filterNames.length; i++) {
        if(filterNames[i].isExact){
          filterString +=
            "OR `$.ObjectMeta.Name` = '" + filterNames[i].value.trim() + "'";
        }
        else{
          filterString +=
            "OR `$.ObjectMeta.Name` LIKE '" + filterNames[i].value.trim() + "%' ";
        }   
      }
      filterString += ")";
    } else {
        if(filterNames[0].isExact)
          filterString += "`$.ObjectMeta.Name` = '" + filterNames[0].value.trim() + "'";
        else
          filterString += "`$.ObjectMeta.Name` LIKE '" + filterNames[0].value.trim() + "%' ";
    }
  }
  if (filterNames && filterNames.length > 0 && (typeValue || statusValue)) {
    filterString += " AND ";
  }
  if (typeValue) {
    filterString += "`$.Spec.Type` = '" + typeValue.toLowerCase() + "'";
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
    filterString += "`$.Status.Phase` = '" + status + "'";
  }
  let orderByString = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
        break;
      case 1:
        orderByString = "`$.ObjectMeta.Name` ";
        break;
      case 2:
        break;
      case 3:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_messages_in')].Value` ";
        break;
      case 4:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_messages_out')].Value` ";
        break;
      case 5:
        orderByString =
          "`$.Metrics[?(@.Name=='enmasse_messages_stored')].Value` ";
        break;
      case 6:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_senders')].Value` ";
        break;
      case 7:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_receivers')].Value` ";
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
        ObjectMeta {
          Namespace
          Name
        }
        Spec {
          Address
          Type
          Plan {
            Spec {
              DisplayName
            }
            ObjectMeta {
              Name
            }
          }
        }
        Status {
          PlanStatus{
            Partitions
          }
          Phase
          IsReady
          Messages
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
  return ALL_ADDRESS_FOR_ADDRESS_SPACE;
};

export const RETURN_ADDRESS_SPACE_DETAIL = (
  name?: string,
  namespace?: string
) => {
  const ADDRESS_SPACE_DETAIL = gql`
    query all_address_spaces {
      addressSpaces(
        filter: "\`$..Name\` = '${name}' AND \`$..Namespace\` = '${namespace}'"
      ) {
        AddressSpaces {
          ObjectMeta {
            Namespace
            Name
            CreationTimestamp
          }
          Spec {
            Type
            Plan {
              ObjectMeta {
                Name
              }
              Spec {
                DisplayName
              }   
            }
          }
          Status {
            IsReady
            Messages
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
        filter: "\`$..Name\` = '${name}' AND \`$..Namespace\` = '${namespace}'"
      ) {
        AddressSpaces {
          Spec {
            Plan {
              ObjectMeta {
                Name
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
    filter += "`$.ObjectMeta.Name` LIKE '" + addressSpace + ".%' AND ";
  }
  if (namespace) {
    filter += "`$.ObjectMeta.Namespace` = '" + namespace + "' AND ";
  }
  if (addressName) {
    filter += "`$.ObjectMeta.Name` = '" + addressName + "'";
  }
  const ADDRESSDETAIL = gql`
  query single_addresses {
    addresses(
      filter: "${filter}" 
    ) {
      Total
      Addresses {
        ObjectMeta {
          Namespace
          Name
          CreationTimestamp
        }
        Spec {
          Address
          Plan {
            Spec {
              DisplayName
              AddressType
            }
          }
        }
        Status {
          IsReady
          Messages
          Phase
          PlanStatus {
            Partitions
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
    filter += "`$.ObjectMeta.Name` LIKE '" + addressSpace + ".%' AND ";
  }
  if (namespace) {
    filter += "`$.ObjectMeta.Namespace` = '" + namespace + "' AND ";
  }
  if (addressName) {
    filter += "`$.ObjectMeta.Name` = '" + addressName + "'";
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
        orderByString = "`$.ObjectMeta.Name` ";
        break;
      case 3:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_messages_in')].Value` ";
        break;
      case 4:
        orderByString =
          "`$.Metrics[?(@.Name=='enmasse_messages_backlog')].Value` ";
        break;
    }
    orderByString += sortBy.direction;
  }

  let filterForLink = "";
  if (filterNames && filterNames.length > 0) {
    if (filterNames.length > 1) {
      if(filterNames[0].isExact)
        filterForLink += "(`$.ObjectMeta.Name` = '" + filterNames[0].value.trim() + "'";
      else
        filterForLink += "(`$.ObjectMeta.Name` LIKE '" + filterNames[0].value.trim() + "%' ";
      for (let i = 1; i < filterNames.length; i++) {
        if(filterNames[i].isExact)
          filterForLink +=
            "OR `$.ObjectMeta.Name` = '" + filterNames[i].value.trim() + "'";
        else
          filterForLink +=
            "OR `$.ObjectMeta.Name` LIKE '" + filterNames[i].value.trim() + "%' ";
      }
      filterForLink += ")";
    } else {
        if(filterNames[0].isExact)
          filterForLink += "(`$.ObjectMeta.Name` = '" + filterNames[0].value.trim() + "')";
        else
          filterForLink += "(`$.ObjectMeta.Name` LIKE '" + filterNames[0].value.trim() + "%')";
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
      if(filterContainers[0].isExact)
        filterForLink +=
          "(`$.Spec.Connection.Spec.ContainerId` = '" +
          filterContainers[0].value.trim() +
          "'";
      else
        filterForLink +=
          "(`$.Spec.Connection.Spec.ContainerId` LIKE '" +
          filterContainers[0].value.trim() +
          "%'";
      for (let i = 1; i < filterContainers.length; i++) {
        if(filterContainers[i].isExact)
          filterForLink +=
            "OR `$.Spec.Connection.Spec.ContainerId` = '" +
            filterContainers[i].value.trim() +
            "'";
        else
          filterForLink +=
            "OR `$.Spec.Connection.Spec.ContainerId` LIKE '" +
            filterContainers[i].value.trim() +
            "%";
      }
      filterForLink += ")";
    } else {
      if(filterContainers[0].isExact)
        filterForLink +=
          "(`$.Spec.Connection.Spec.ContainerId` = '" +
          filterContainers[0].value.trim() +
          "')";
      else
        filterForLink +=
          "(`$.Spec.Connection.Spec.ContainerId` LIKE '" +
          filterContainers[0].value.trim() +
          "%')";
    }
    if (filterRole && filterRole.trim() != "") {
      filterForLink += " AND ";
    }
  }

  if (filterRole && filterRole.trim() != "") {
    filterForLink +=
      "`$.Spec.Role` = '" + filterRole.trim().toLowerCase() + "' ";
  }

  const query = gql`
  query single_address_with_links_and_metrics {
    addresses(
      filter: "${filter}"
    ) {
      Total
      Addresses {
        ObjectMeta {
          Name
        }
        Spec {
          AddressSpace
        }
        Links (first:${perPage} offset:${perPage *
    (page - 1)}  orderBy:"${orderByString}" filter:"${filterForLink}"){
          Total
          Links {
            ObjectMeta {
              Name
            }
            Spec {
              Role
              Connection {
                ObjectMeta{
                  Name
                  Namespace
                }
                Spec {
                  ContainerId
                }
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
      ObjectMeta {
        Name
      }
      Spec {
        AddressType
        DisplayName
        LongDescription
        ShortDescription
      }
    }
  }
`;
  return ADDRESS_PLANS;
};

export const CREATE_ADDRESS = gql`
  mutation create_addr($a: Address_enmasse_io_v1beta1_Input!) {
    createAddress(input: $a) {
      Name
      Namespace
      Uid
    }
  }
`;

export const CREATE_ADDRESS_SPACE = gql`
  mutation create_as($as: AddressSpace_enmasse_io_v1beta1_Input!) {
    createAddressSpace(input: $as) {
      Name
      Uid
      CreationTimestamp
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

export const ADDRESS_COMMAND_PRIVEW_DETAIL = gql`
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
    filter += "`$.Spec.AddressSpace` = '" + name + "'";
  }
  if (namespace) {
    filter +=
      " AND `$.ObjectMeta.Namespace` = '" + namespace + "'";
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
        filter += "(`$.Spec.Hostname` = '" + hostnames[0].value.trim() + "'";
      else
        filter += "(`$.Spec.Hostname` LIKE '" + hostnames[0].value.trim() + "%' ";
      for (let i = 1; i < hostnames.length; i++) {
        if (hostnames[i].isExact)
          filter += "OR `$.Spec.Hostname` = '" + hostnames[i].value.trim() + "'";
        else
          filter += "OR `$.Spec.Hostname` LIKE '" + hostnames[i].value.trim() + "%' ";
      }
      filter += ")";
    } else {
        if (hostnames[0].isExact)
          filter += "(`$.Spec.Hostname` = '" + hostnames[0].value.trim() + "')";
        else
          filter += "(`$.Spec.Hostname` LIKE '" + hostnames[0].value.trim() + "%')";
    }
  }

  if (containers && containers.length > 0) {
    if (hostnames && hostnames.length > 0) {
      filter += " AND ";
    }
    if (containers.length > 1) {
      if (containers[0].isExact)
        filter += "(`$.Spec.ContainerId` = '" + containers[0].value.trim() + "'";
      else
        filter += "(`$.Spec.ContainerId` LIKE '" + containers[0].value.trim() + "%' ";
      for (let i = 1; i < containers.length; i++) {
        if (containers[i].isExact)
          filter += "OR `$.Spec.ContainerId` = '" + containers[i].value.trim() + "'";
        else
          filter += "OR `$.Spec.ContainerId` LIKE '" + containers[i].value.trim() + "%' ";
      }
      filter += ")";
    } else {
        if (containers[0].isExact)
          filter += "(`$.Spec.ContainerId` = '" + containers[0].value.trim() + "')";
        else
          filter += "(`$.Spec.ContainerId` LIKE '" + containers[0].value.trim() + "%')";
    }
  }

  let orderByString = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
        orderByString = "`$.Spec.Hostname` ";
        break;
      case 1:
        orderByString = "`$.Spec.ContainerId` ";
        break;
      case 2:
        orderByString = "`$.Spec.Protocol` ";
        break;
      case 3:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_messages_in')].Value` ";
        break;
      case 4:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_messages_out')].Value` ";
        break;
      case 5:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_senders')].Value` ";
        break;
      case 6:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_receivers')].Value` ";
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
        ObjectMeta {
          Name
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

export const RETURN_CONNECTION_DETAIL = (
  addressSpaceName?: string,
  addressSpaceNameSpcae?: string,
  connectionName?: string
) => {
  let filter = "";
  if (addressSpaceName) {
    filter +=
      "`$.Spec.AddressSpace` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpcae) {
    filter +=
      "`$.ObjectMeta.Namespace` = '" +
      addressSpaceNameSpcae +
      "' AND ";
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
          Protocol,
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

export const RETURN_ADDRESS_TYPES = gql`
  query addressTypes {
    addressTypes_v2(addressSpaceType: standard) {
      ObjectMeta {
        Name
      }
      Spec {
        DisplayName
        LongDescription
        ShortDescription
      }
    }
  }
`;
export const RETURN_NAMESPACES = gql`
  query all_namespaces {
    namespaces {
      ObjectMeta {
        Name
      }
      Status {
        Phase
      }
    }
  }
`;

export const RETURN_ADDRESS_SPACE_PLANS = gql`
  query all_address_space_plans {
    addressSpacePlans {
      ObjectMeta {
        Name
        Uid
        CreationTimestamp
      }
      Spec {
        AddressSpaceType
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
    filter +=
      "`$.Spec.AddressSpace` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpcae) {
    filter +=
      "`$.ObjectMeta.Namespace` = '" +
      addressSpaceNameSpcae +
      "' AND ";
  }
  if (connectionName) {
    filter += "`$.ObjectMeta.Name` = '" + connectionName + "'";
  }
  let orderByString = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
        orderByString = "";
        break;
      case 1:
        orderByString = "`$.ObjectMeta.Name` ";
        break;
      case 2:
        orderByString = "`$.Spec.Address` ";
        break;
      case 3:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_deliveries')].Value` ";
        break;
      case 4:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_rejected')].Value` ";
        break;
      case 5:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_released')].Value` ";
        break;
      case 6:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_modified')].Value` ";
        break;
      case 7:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_presettled')].Value` ";
        break;
      case 8:
        orderByString = "`$.Metrics[?(@.Name=='enmasse_undelivered')].Value` ";
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
        filterForLink += "(`$.ObjectMeta.Name` = '" + filterNames[0].value.trim() + "'";
      else
        filterForLink += "(`$.ObjectMeta.Name` LIKE '" + filterNames[0].value.trim() + "%' ";
      for (let i = 1; i < filterNames.length; i++) {
        if (filterNames[i].isExact)
          filterForLink +=
            "OR `$.ObjectMeta.Name` = '" + filterNames[i].value.trim() + "'";
        else
          filterForLink +=
            "OR `$.ObjectMeta.Name` LIKE '" + filterNames[i].value.trim() + "%' ";
      }
      filterForLink += ")";
    } else {
        if (filterNames[0].isExact)
          filterForLink += "`$.ObjectMeta.Name` = '" + filterNames[0].value.trim() + "'";
        else
          filterForLink += "`$.ObjectMeta.Name` LIKE '" + filterNames[0].value.trim() + "%' ";
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
        filterForLink += "(`$.Spec.Address` = '" + filterAddresses[0].value.trim() + "'";
      else
        filterForLink += "(`$.Spec.Address` LIKE '" + filterAddresses[0].value.trim() + "%' ";
      for (let i = 1; i < filterAddresses.length; i++) {
        if (filterAddresses[i].isExact)
          filterForLink +=
            "OR `$.Spec.Address` = '" + filterAddresses[i].value.trim() + "'";
        else
          filterForLink +=
            "OR `$.Spec.Address` LIKE '" + filterAddresses[i].value.trim() + "%' ";
      }
      filterForLink += ")";
    } else {
        if (filterAddresses[0].isExact)
          filterForLink += "`$.Spec.Address` = '" + filterAddresses[0].value.trim() + "'";
        else
          filterForLink += "`$.Spec.Address` LIKE '" + filterAddresses[0].value.trim() + "%' ";
    }
    if (filterRole && filterRole.trim() != "") {
      filterForLink += " AND ";
    }
  }

  if (filterRole && filterRole.trim() != "") {
    filterForLink +=
      "`$.Spec.Role` = '" + filterRole.trim().toLowerCase() + "' ";
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
        }
        Links(first:${perPage} offset:${perPage *
    (page - 1)} orderBy:"${orderByString}"
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

export const RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION = (
  name: string,
  namespace: string,
  type: string
) => {
  let filterString = "";
  if (name && name.trim() !== "") {
    filterString += "`$.ObjectMeta.Name` LIKE '" + name + ".%' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filterString += "`$.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  if (type.trim().toLowerCase() === "subscription") {
    filterString += " AND `$.Spec.Type` = 'topic'";
  }
  const ALL_TOPICS_FOR_ADDRESS_SPACE = gql`
  query all_addresses_for_addressspace_view {
    addresses(
      filter:"${filterString}"
    ) {
      Total
      Addresses {
        ObjectMeta {
          Namespace
          Name
        }
        Spec {
          Address
          Type
          Plan {
            Spec {
              DisplayName
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
      filter: "\`$.ObjectMeta.Name\` = '${addressname}' AND \`$.ObjectMeta.Namespace\` = '${namespace}'"
    ) {
      Total
      Addresses {
        Links(filter:"\`$.ObjectMeta.Name\` LIKE '${name}%'" first:10,offset:0)  {
          Total
          Links{
            ObjectMeta {
              Name
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
      filter: "\`$.ObjectMeta.Name\` = '${addressname}' AND \`$.ObjectMeta.Namespace\` = '${namespace}'"
    ) {
      Total
      Addresses {
        Links(filter:"\`$.Spec.Connection.Spec.ContainerId\` LIKE '${container}%'" first:10 ,offset:0)  {
          Total
          Links{
            Spec{
              Connection{
                Spec{
                  ContainerId
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
    filter +=
      "`$.ObjectMeta.Namespace` = '" + namespace + "' AND ";
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

export const RETURN_ALL_CONNECTION_LINKS_FOR_ADDRESS_SEARCH = (
  connectionName: string,
  namespace: string,
  address: string
) => {
  let filter = "";
  if (namespace) {
    filter +=
      "`$.ObjectMeta.Namespace` = '" + namespace + "' AND ";
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

export const RETURN_ALL_ADDRESS_SPACES_FOR_NAME_OR_NAMESPACE = (
  isName: boolean,
  value: string
) => {
  let filter = "";
  if (value) {
    if (isName) {
      filter += "`$.ObjectMeta.Name` LIKE '" + value + "%'";
    } else {
      filter += "`$.ObjectMeta.Namespace` LIKE '" + value + "%'";
    }
  }
  const all_address_spaces = gql`
  query all_address_spaces {
    addressSpaces(filter: "${filter}"  
    first:100 offset:0) {
      Total
      AddressSpaces {
        ObjectMeta {
          Namespace
          Name
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
    filterString += "`$.Spec.AddressSpace` = '" + addressspaceName + "' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filterString += "`$.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  if (name && name.trim() != "") {
    filterString += " AND ";
    filterString += "`$.ObjectMeta.Name` LIKE '" + name.trim() + "%' ";
  }
  const ALL_ADDRESS_FOR_ADDRESS_SPACE = gql`
  query all_addresses_for_addressspace_view {
    addresses( first:10 offset:0
      filter:"${filterString}"
    ) {
      Total
      Addresses {
        ObjectMeta {
          Name
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
    filter += "`$.Spec.AddressSpace` = '" + name + "'";
  }
  if (namespace) {
    filter +=
      " AND `$.ObjectMeta.Namespace` = '" + namespace + "'";
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

export const RETURN_AUTHENTICATION_SERVICES = gql
  `query addressspace_schema {
    addressSpaceSchema_v2  {
      ObjectMeta {
        Name
      }
      Spec {
        AuthenticationServices
      }
    }
  }
`;
