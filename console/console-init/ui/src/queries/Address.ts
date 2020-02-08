/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { ISortBy } from "@patternfly/react-table";

const DELETE_ADDRESS = gql`
  mutation delete_addr($a: ObjectMeta_v1_Input!) {
    deleteAddress(input: $a)
  }
`;

const PURGE_ADDRESS = gql`
  mutation purge_addr($a: ObjectMeta_v1_Input!) {
    purgeAddress(input: $a)
  }
`;

const ALL_ADDRESS_FOR_ADDRESS_SPACE_FILTER = (
  name?: string,
  namespace?: string,
  filterNames?: any[],
  typeValue?: string | null,
  statusValue?: string | null
) => {
  let filter="";
  let filterNamesLength=filterNames && filterNames.length;
  let filterName=filterNames && filterNames[0];
  let filterNameValue=filterName && filterName.value && filterName.value.trim();
  
  if (name && name.trim() !== "") {
    filter += "`$.ObjectMeta.Name` LIKE '" + name + ".%' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filter += "`$.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  if ((filterNamesLength && filterNamesLength > 0) || typeValue || statusValue) {
    filter += " AND ";
  }
  if (filterNamesLength && filterNamesLength> 0) {
    if (filterNamesLength > 1) {
      if (filterName.isExact)
      filter +=
          "(`$.Spec.Address` = '" + filterNameValue + "'";
      else
      filter +=
          "(`$.Spec.Address` LIKE '" + filterNameValue + "%' ";
      for (let i = 1; i < filterNamesLength; i++) {
        let filterName=filterNames && filterNames[i];
        let filterNameValue=filterName && filterName.value.trim();
        if (filterName.isExact) {
          filter +=
            "OR `$.Spec.Address` = '" + filterNameValue + "'";
        } else {
          filter +=
            "OR `$.Spec.Address` LIKE '" + filterNameValue + "%' ";
        }
      }
      filter += ")";
    } else {
      if (filterName.isExact)
      filter +=
          "`$.Spec.Address` = '" + filterNameValue + "'";
      else
      filter +=
          "`$.Spec.Address` LIKE '" + filterNameValue + "%' ";
    }
  }
  if (filterName && filterName > 0 && (typeValue || statusValue)) {
    filter += " AND ";
  }
  if (typeValue) {
    filter += "`$.Spec.Type` = '" + typeValue.toLowerCase() + "'";
  }
  if (typeValue && statusValue) {
    filter += " AND ";
  }
  if (statusValue) {
    let status = "";
    if (statusValue === "Failed") {
      status = "Pending";
    } else {
      status = statusValue;
    }
    filter += "`$.Status.Phase` = '" + status + "'";
  }
  return filter;
};

const ALL_ADDRESS_FOR_ADDRESS_SPACE_SORT=(sortBy?: ISortBy)=>{
  let orderBy="";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
      case 2:
      case 3:
        break;
      case 1:
        orderBy = "`$.ObjectMeta.Name` ";
        break;
      case 4:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_messages_in')].Value` ";
        break;
      case 5:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_messages_out')].Value` ";
        break;
      case 6:
        orderBy =
          "`$.Metrics[?(@.Name=='enmasse_messages_stored')].Value` ";
        break;
      case 7:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_senders')].Value` ";
        break;
      case 8:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_receivers')].Value` ";
        break;
      default:
        break;
    }
    if (orderBy !== "" && sortBy.direction) {
      orderBy += sortBy.direction;
    }
  }
  return orderBy;
};

const RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE = (
  page: number,
  perPage: number,
  name?: string,
  namespace?: string,
  filterNames?: any[],
  typeValue?: string | null,
  statusValue?: string | null,
  sortBy?: ISortBy
) => {
  let filter =ALL_ADDRESS_FOR_ADDRESS_SPACE_FILTER(name,namespace,filterNames,typeValue,statusValue);
  let orderBy =ALL_ADDRESS_FOR_ADDRESS_SPACE_SORT(sortBy);
  
  const ALL_ADDRESS_FOR_ADDRESS_SPACE = gql`
    query all_addresses_for_addressspace_view {
      addresses( first:${perPage} offset:${perPage * (page - 1)}
        filter:"${filter}"
        orderBy:"${orderBy}"
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

const CURRENT_ADDRESS_SPACE_PLAN = (name?: string, namespace?: string) => {
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

const RETURN_ADDRESS_DETAIL = (
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
            Topic
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

const ADDRESS_LINKS_FILTER=(
  filterNames: any[],
  filterContainers: any[],
  addressSpace?: string,
  namespace?: string,
  addressName?: string,
  filterRole?: string
)=>{
  let filter="",filterForLink="";
  let filterNamesLength=filterNames && filterNames.length;
  let filterName=filterNames && filterNames[0];
  let filterNameValue=filterName && filterName.value && filterName.value.trim();

  let filterContainersLength=filterContainers && filterContainers.length;
  let filterContainer=filterContainers && filterContainers[0];
  let filterContainerValue=filterContainer && filterContainer.value && filterContainer.value.trim();

  if (addressSpace) {
    filter += "`$.ObjectMeta.Name` LIKE '" + addressSpace + ".%' AND ";
  }
  if (namespace) {
    filter += "`$.ObjectMeta.Namespace` = '" + namespace + "' AND ";
  }
  if (addressName) {
    filter += "`$.ObjectMeta.Name` = '" + addressName + "'";
  }

  //links filter
  if (filterNamesLength> 0) {
    if (filterNamesLength > 1) {
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
          "(`$.ObjectMeta.Name` = '" + filterNameValue + "')";
      else
        filterForLink +=
          "(`$.ObjectMeta.Name` LIKE '" + filterNameValue + "%')";
    }
    if (
      (filterContainersLength > 0) ||
      (filterRole && filterRole.trim() != "")
    ) {
      filterForLink += " AND ";
    }
  }
  if (filterContainersLength > 0) {
    if (filterContainersLength > 1) {
      if (filterContainer.isExact)
        filterForLink +=
          "(`$.Spec.Connection.Spec.ContainerId` = '" +
          filterContainerValue +
          "'";
      else
        filterForLink +=
          "(`$.Spec.Connection.Spec.ContainerId` LIKE '" +
          filterContainerValue +
          "%'";
      for (let i = 1; i < filterContainers.length; i++) {
        let filterContainer=filterContainers && filterContainers[i];
        let filterContainerValue=filterContainer && filterContainer.value && filterContainer.value.trim();
        if (filterContainer.isExact)
          filterForLink +=
            "OR `$.Spec.Connection.Spec.ContainerId` = '" +
            filterContainerValue +
            "'";
        else
          filterForLink +=
            "OR `$.Spec.Connection.Spec.ContainerId` LIKE '" +
            filterContainerValue +
            "%";
      }
      filterForLink += ")";
    } else {
      if (filterContainer.isExact)
        filterForLink +=
          "(`$.Spec.Connection.Spec.ContainerId` = '" +
          filterContainerValue +
          "')";
      else
        filterForLink +=
          "(`$.Spec.Connection.Spec.ContainerId` LIKE '" +
          filterContainerValue +
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

  return {filter,filterForLink};
}

const ADDRESS_LINKS_SORT=(sortBy?: ISortBy,)=>{
  let orderBy="";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
      case 1:
        orderBy = "";
        break;
      case 2:
        orderBy = "`$.ObjectMeta.Name` ";
        break;
      case 3:
        orderBy = "`$.Metrics[?(@.Name=='enmasse_messages_in')].Value` ";
        break;
      case 4:
        orderBy =
          "`$.Metrics[?(@.Name=='enmasse_messages_backlog')].Value` ";
        break;
      default:
        break;
    }
    orderBy += sortBy.direction;
  }
  return orderBy;
}

const RETURN_ADDRESS_LINKS = (
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
  const {filter,filterForLink} =ADDRESS_LINKS_FILTER(filterNames,filterContainers,addressSpace,namespace,addressName,filterRole);
  const orderBy =ADDRESS_LINKS_SORT(sortBy);
 
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
    (page - 1)}  orderBy:"${orderBy}" filter:"${filterForLink}"){
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

const RETURN_ADDRESS_PLANS = (
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

const CREATE_ADDRESS = gql`
  mutation create_addr($a: Address_enmasse_io_v1beta1_Input!) {
    createAddress(input: $a) {
      Name
      Namespace
      Uid
    }
  }
`;

const EDIT_ADDRESS = gql`
  mutation patch_addr(
    $a: ObjectMeta_v1_Input!
    $jsonPatch: String!
    $patchType: String!
  ) {
    patchAddress(input: $a, jsonPatch: $jsonPatch, patchType: $patchType)
  }
`;

const ADDRESS_COMMAND_PRIVIEW_DETAIL = gql`
  query cmd($a: Address_enmasse_io_v1beta1_Input!) {
    addressCommand(input: $a)
  }
`;

const RETURN_ADDRESS_TYPES = gql`
  query addressTypes($a: AddressSpaceType!) {
    addressTypes_v2(addressSpaceType: $a) {
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

const RETURN_ADDRESS_SPACE_PLANS = gql`
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

const RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION = (
  name: string,
  namespace: string,
  type: string
) => {
  let filter = "";
  if (name && name.trim() !== "") {
    filter += "`$.ObjectMeta.Name` LIKE '" + name + ".%' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filter += "`$.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  if (type.trim().toLowerCase() === "subscription") {
    filter += " AND `$.Spec.Type` = 'topic'";
  }
  const ALL_TOPICS_FOR_ADDRESS_SPACE = gql`
    query all_addresses_for_addressspace_view {
      addresses(
        filter:"${filter}"
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

const RETURN_ALL_NAMES_OF_ADDRESS_LINK_FOR_TYPEAHEAD_SEARCH = (
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

const RETURN_ALL_CONTAINER_IDS_OF_ADDRESS_LINKS_FOR_TYPEAHEAD_SEARCH = (
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

const RETURN_ALL_ADDRESS_NAMES_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH = (
  addressspaceName?: string,
  namespace?: string,
  name?: string
) => {
  let filter = "";
  if (addressspaceName && addressspaceName.trim() !== "") {
    filter += "`$.Spec.AddressSpace` = '" + addressspaceName + "' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filter += "`$.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  if (name && name.trim() != "") {
    filter += " AND ";
    filter += "`$.Spec.Address` LIKE '" + name.trim() + "%' ";
  }
  const ALL_ADDRESS_FOR_ADDRESS_SPACE = gql`
    query all_addresses_for_addressspace_view {
      addresses( first:10 offset:0
        filter:"${filter}"
      ) {
        Total
        Addresses {
          Spec{
            Address
          }
        }
      }
    }
  `;
  return ALL_ADDRESS_FOR_ADDRESS_SPACE;
};

export {
  DELETE_ADDRESS,
  PURGE_ADDRESS,
  RETURN_ALL_ADDRESS_FOR_ADDRESS_SPACE,
  CURRENT_ADDRESS_SPACE_PLAN,
  RETURN_ADDRESS_DETAIL,
  RETURN_ADDRESS_LINKS,
  RETURN_ADDRESS_PLANS,
  CREATE_ADDRESS,
  EDIT_ADDRESS,
  ADDRESS_COMMAND_PRIVIEW_DETAIL,
  RETURN_ADDRESS_TYPES,
  RETURN_ADDRESS_SPACE_PLANS,
  RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION,
  RETURN_ALL_NAMES_OF_ADDRESS_LINK_FOR_TYPEAHEAD_SEARCH,
  RETURN_ALL_CONTAINER_IDS_OF_ADDRESS_LINKS_FOR_TYPEAHEAD_SEARCH,
  RETURN_ALL_ADDRESS_NAMES_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH
};
