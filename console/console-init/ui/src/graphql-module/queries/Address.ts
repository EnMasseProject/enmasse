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
  let filter = "";
  let filterNamesLength = filterNames && filterNames.length;
  let filterName = filterNames && filterNames[0];
  let filterNameValue =
    filterName && filterName.value && filterName.value.trim();

  if (name && name.trim() !== "") {
    filter += "`$.metadata.name` LIKE '" + name + ".%' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filter += "`$.metadata.namespace` = '" + namespace + "'";
  }
  if (
    (filterNamesLength && filterNamesLength > 0) ||
    typeValue ||
    statusValue
  ) {
    filter += " AND ";
  }
  if (filterNamesLength && filterNamesLength > 0) {
    if (filterNamesLength > 1) {
      if (filterName.isExact)
        filter += "(`$.spec.address` = '" + filterNameValue + "'";
      else filter += "(`$.spec.address` LIKE '" + filterNameValue + "%' ";
      for (let i = 1; i < filterNamesLength; i++) {
        let filterName = filterNames && filterNames[i];
        let filterNameValue = filterName && filterName.value.trim();
        if (filterName.isExact) {
          filter += "OR `$.spec.address` = '" + filterNameValue + "'";
        } else {
          filter += "OR `$.spec.address` LIKE '" + filterNameValue + "%' ";
        }
      }
      filter += ")";
    } else {
      if (filterName.isExact)
        filter += "`$.spec.address` = '" + filterNameValue + "'";
      else filter += "`$.spec.address` LIKE '" + filterNameValue + "%' ";
    }
  }
  if (
    filterNamesLength &&
    filterNamesLength > 0 &&
    (typeValue || statusValue)
  ) {
    filter += " AND ";
  }
  if (typeValue) {
    filter += "`$.spec.type` = '" + typeValue.toLowerCase() + "'";
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
    filter += "`$.status.phase` = '" + status + "'";
  }
  return filter;
};

const ALL_ADDRESS_FOR_ADDRESS_SPACE_SORT = (sortBy?: ISortBy) => {
  let orderBy = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
      case 2:
      case 3:
        break;
      case 1:
        orderBy = "`$.metadata.name` ";
        break;
      case 4:
        orderBy = "`$.metadata.creationTimestamp`";
        break;
      case 5:
        orderBy = "`$.metrics[?(@.name=='enmasse_messages_in')].value` ";
        break;
      case 6:
        orderBy = "`$.metrics[?(@.name=='enmasse_messages_out')].value` ";
        break;
      case 7:
        orderBy = "`$.metrics[?(@.name=='enmasse_messages_stored')].value` ";
        break;
      case 8:
        orderBy = "`$.metrics[?(@.name=='enmasse_senders')].value` ";
        break;
      case 9:
        orderBy = "`$.metrics[?(@.name=='enmasse_receivers')].value` ";
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
  let filter = ALL_ADDRESS_FOR_ADDRESS_SPACE_FILTER(
    name,
    namespace,
    filterNames,
    typeValue,
    statusValue
  );
  let orderBy = ALL_ADDRESS_FOR_ADDRESS_SPACE_SORT(sortBy);

  const ALL_ADDRESS_FOR_ADDRESS_SPACE = gql`
    query all_addresses_for_addressspace_view {
      addresses( first:${perPage} offset:${perPage * (page - 1)}
        filter:"${filter}"
        orderBy:"${orderBy}"
      ) {
        total
        addresses {
          metadata {
            namespace
            name
            creationTimestamp
          }
          spec {
            address
            type
            plan {
              spec {
                displayName
              }
              metadata {
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

const CURRENT_ADDRESS_SPACE_PLAN = (name?: string, namespace?: string) => {
  const ADDRESS_SPACE_PLAN = gql`
      query all_address_spaces {
        addressSpaces(
          filter: "\`$.metadata.name\` = '${name}' AND \`$.metadata.namespace\` = '${namespace}'"
        ) {
          addressSpaces {
            spec {
              plan {
                metadata {
                  name
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
    filter += "`$.metadata.name` LIKE '" + addressSpace + ".%' AND ";
  }
  if (namespace) {
    filter += "`$.metadata.namespace` = '" + namespace + "' AND ";
  }
  if (addressName) {
    filter += "`$.metadata.name` = '" + addressName + "'";
  }
  const ADDRESSDETAIL = gql`
    query single_addresses {
      addresses(
        filter: "${filter}" 
      ) {
        total
        addresses {
          metadata {
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
              metadata{
                name
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

const ADDRESS_LINKS_FILTER = (
  filterNames: any[],
  filterContainers: any[],
  addressSpace?: string,
  namespace?: string,
  addressName?: string,
  filterRole?: string
) => {
  let filter = "",
    filterForLink = "";
  let filterNamesLength = filterNames && filterNames.length;
  let filterName = filterNames && filterNames[0];
  let filterNameValue =
    filterName && filterName.value && filterName.value.trim();

  let filterContainersLength = filterContainers && filterContainers.length;
  let filterContainer = filterContainers && filterContainers[0];
  let filterContainerValue =
    filterContainer && filterContainer.value && filterContainer.value.trim();

  if (addressSpace) {
    filter += "`$.metadata.name` LIKE '" + addressSpace + ".%' AND ";
  }
  if (namespace) {
    filter += "`$.metadata.namespace` = '" + namespace + "' AND ";
  }
  if (addressName) {
    filter += "`$.metadata.name` = '" + addressName + "'";
  }

  //links filter
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
        filterForLink += "(`$.metadata.name` = '" + filterNameValue + "')";
      else
        filterForLink += "(`$.metadata.name` LIKE '" + filterNameValue + "%')";
    }
    if (
      filterContainersLength > 0 ||
      (filterRole && filterRole.trim() !== "")
    ) {
      filterForLink += " AND ";
    }
  }
  if (filterContainersLength > 0) {
    if (filterContainersLength > 1) {
      if (filterContainer.isExact)
        filterForLink +=
          "(`$.spec.connection.spec.containerId` = '" +
          filterContainerValue +
          "'";
      else
        filterForLink +=
          "(`$.spec.connection.spec.containerId` LIKE '" +
          filterContainerValue +
          "%'";
      for (let i = 1; i < filterContainers.length; i++) {
        let filterContainer = filterContainers && filterContainers[i];
        let filterContainerValue =
          filterContainer &&
          filterContainer.value &&
          filterContainer.value.trim();
        if (filterContainer.isExact)
          filterForLink +=
            "OR `$.spec.connection.spec.containerId` = '" +
            filterContainerValue +
            "'";
        else
          filterForLink +=
            "OR `$.spec.connection.spec.containerId` LIKE '" +
            filterContainerValue +
            "%";
      }
      filterForLink += ")";
    } else {
      if (filterContainer.isExact)
        filterForLink +=
          "(`$.spec.connection.spec.containerId` = '" +
          filterContainerValue +
          "')";
      else
        filterForLink +=
          "(`$.spec.connection.spec.containerId` LIKE '" +
          filterContainerValue +
          "%')";
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

const ADDRESS_LINKS_SORT = (sortBy?: ISortBy) => {
  let orderBy = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
      case 1:
        orderBy = "";
        break;
      case 2:
        orderBy = "`$.metadata.name` ";
        break;
      case 3:
        orderBy = "`$.metrics[?(@.name=='enmasse_messages_in')].value` ";
        break;
      case 4:
        orderBy = "`$.metrics[?(@.name=='enmasse_messages_backlog')].value` ";
        break;
      default:
        break;
    }
    orderBy += sortBy.direction;
  }
  return orderBy;
};

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
  const { filter, filterForLink } = ADDRESS_LINKS_FILTER(
    filterNames,
    filterContainers,
    addressSpace,
    namespace,
    addressName,
    filterRole
  );
  const orderBy = ADDRESS_LINKS_SORT(sortBy);
  const query = gql`
    query single_address_with_links_and_metrics {
      addresses(
        filter: "${filter}"
      ) {
        total
        addresses {
          metadata {
            name
          }
          spec {
            addressSpace
          }
          links (first:${perPage} offset:${perPage *
    (page - 1)}  orderBy:"${orderBy}" filter:"${filterForLink}"){
            total
            links {
              metadata {
                name
              }
              spec {
                role
                connection {
                  metadata {
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

const RETURN_ADDRESS_PLANS = (
  addressSpacePlan: string,
  addressType: string
) => {
  const ADDRESS_PLANS = gql`
    query all_address_plans {
      addressPlans(addressSpacePlan: "${addressSpacePlan}", addressType: ${addressType}) {
        metadata {
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

const CREATE_ADDRESS = gql`
  mutation create_addr($a: Address_enmasse_io_v1beta1_Input!, $as: String) {
    createAddress(input: $a, addressSpace: $as) {
      name
      namespace
      uid
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

const ADDRESS_COMMAND_PREVIEW_DETAIL = gql`
  query cmd($a: Address_enmasse_io_v1beta1_Input!, $as: String) {
    addressCommand(input: $a, addressSpace: $as)
  }
`;

const RETURN_ADDRESS_TYPES = gql`
  query addressTypes($a: AddressSpaceType!) {
    addressTypes_v2(addressSpaceType: $a) {
      metadata {
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

const RETURN_ADDRESS_SPACE_PLANS = gql`
  query all_address_space_plans {
    addressSpacePlans {
      metadata {
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

const RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION = (
  name: string,
  namespace: string,
  type: string
) => {
  let filter = "";
  if (name && name.trim() !== "") {
    filter += "`$.metadata.name` LIKE '" + name + ".%' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filter += "`$.metadata.namespace` = '" + namespace + "'";
  }
  if (type.trim().toLowerCase() === "subscription") {
    filter += " AND `$.spec.type` = 'topic'";
  }
  const ALL_TOPICS_FOR_ADDRESS_SPACE = gql`
    query all_addresses_for_addressspace_view {
      addresses(
        filter:"${filter}"
      ) {
        total
        addresses {
          metadata {
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

const RETURN_ALL_NAMES_OF_ADDRESS_LINK_FOR_TYPEAHEAD_SEARCH = (
  addressname: string,
  namespace: string,
  name: string
) => {
  const all_names = gql`
    query all_link_names_for_connection {
      addresses(
        filter: "\`$.metadata.name\` = '${addressname}' AND \`$.metadata.namespace\` = '${namespace}'"
      ) {
        total
        addresses {
          links(filter:"\`$.metadata.name\` LIKE '${name}%'" first:10,offset:0)  {
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
        filter: "\`$.metadata.name\` = '${addressname}' AND \`$.metadata.namespace\` = '${namespace}'"
      ) {
        total
        addresses {
          links(filter:"\`$.spec.connection.spec.containerId\` LIKE '${container}%'" first:10 ,offset:0)  {
            total
            links {
              spec {
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

const RETURN_ALL_ADDRESS_NAMES_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH = (
  addressspaceName?: string,
  namespace?: string,
  name?: string
) => {
  let filter = "";
  if (addressspaceName && addressspaceName.trim() !== "") {
    filter += "`$.spec.addressSpace` = '" + addressspaceName + "' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filter += "`$.metadata.namespace` = '" + namespace + "'";
  }
  if (name && name.trim() !== "") {
    filter += " AND ";
    filter += "`$.spec.address` LIKE '" + name.trim() + "%' ";
  }
  const ALL_ADDRESS_FOR_ADDRESS_SPACE = gql`
    query all_addresses_for_addressspace_view {
      addresses( first:10 offset:0
        filter:"${filter}"
      ) {
        total
        addresses {
          spec{
            address
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
  ADDRESS_COMMAND_PREVIEW_DETAIL,
  RETURN_ADDRESS_TYPES,
  RETURN_ADDRESS_SPACE_PLANS,
  RETURN_TOPIC_ADDRESSES_FOR_SUBSCRIPTION,
  RETURN_ALL_NAMES_OF_ADDRESS_LINK_FOR_TYPEAHEAD_SEARCH,
  RETURN_ALL_CONTAINER_IDS_OF_ADDRESS_LINKS_FOR_TYPEAHEAD_SEARCH,
  RETURN_ALL_ADDRESS_NAMES_OF_ADDRESS_SPACES_FOR_TYPEAHEAD_SEARCH
};
