/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { ISortBy } from "@patternfly/react-table";

const DELETE_ADDRESS_SPACE = gql`
  mutation delete_as($a: ObjectMeta_v1_Input!) {
    deleteAddressSpace(input: $a)
  }
`;

const ALL_ADDRESS_SPACES_FILTER = (
  filterNames?: any[],
  filterNameSpaces?: any[],
  filterType?: string | null
) => {
  let filter = "";
  let filterNamesLength = filterNames && filterNames.length;
  let filterName = filterNames && filterNames[0];
  let filterNameValue =
    filterName && filterName.value && filterName.value.trim();

  let filterNameSpacesLength = filterNameSpaces && filterNameSpaces.length;
  let filterNameSpace = filterNameSpaces && filterNameSpaces[0];
  let filterNameSpaceValue =
    filterNameSpace && filterNameSpace.value && filterNameSpace.value.trim();

  if (filterNamesLength && filterNamesLength > 0) {
    if (filterNamesLength > 1) {
      if (filterName.isExact)
        filter += "(`$.metadata.name` = '" + filterNameValue + "'";
      else filter += "(`$.metadata.name` LIKE '" + filterNameValue + "%'";
      for (let i = 1; i < filterNamesLength; i++) {
        let filterName = filterNames && filterNames[i];
        let filterNameValue =
          filterName && filterName.value && filterName.value.trim();
        if (filterName.isExact)
          filter += "OR `$.metadata.name` = '" + filterNameValue + "'";
        else filter += "OR `$.metadata.name` LIKE '" + filterNameValue + "%'";
      }
      filter += ")";
    } else {
      if (filterName.isExact)
        filter += "`$.metadata.name` = '" + filterNameValue + "'";
      else filter += "`$.metadata.name` LIKE '" + filterNameValue + "%'";
    }
  }
  if (
    filterNamesLength &&
    filterNameSpacesLength &&
    filterNameSpacesLength > 0
  ) {
    filter += " AND ";
  }
  if (filterNameSpacesLength && filterNameSpacesLength > 0) {
    if (filterNameSpacesLength > 1) {
      if (filterNameSpace.isExact) {
        filter +=
          "(`$.metadata.namespace` = '" + filterNameSpaceValue.trim() + "'";
      } else {
        filter +=
          "(`$.metadata.namespace` LIKE '" + filterNameSpaceValue.trim() + "%'";
      }
      for (let i = 1; i < filterNameSpacesLength; i++) {
        let filterNameSpace = filterNameSpaces && filterNameSpaces[i];
        let filterNameSpaceValue =
          filterNameSpace &&
          filterNameSpace.value &&
          filterNameSpace.value.trim();
        if (filterNameSpace.isExact)
          filter +=
            "OR `$.metadata.namespace` = '" + filterNameSpaceValue + "'";
        else
          filter +=
            "OR `$.metadata.namespace` LIKE '" + filterNameSpaceValue + "%'";
      }
      filter += ")";
    } else {
      if (filterNameSpace.isExact)
        filter += "`$.metadata.namespace` = '" + filterNameSpaceValue + "'";
      else
        filter += "`$.metadata.namespace` LIKE '" + filterNameSpaceValue + "%'";
    }
  }
  if (
    ((filterNamesLength && filterNamesLength > 0) ||
      (filterNameSpacesLength && filterNameSpacesLength > 0)) &&
    filterType &&
    filterType.trim() !== ""
  ) {
    filter += " AND ";
  }
  if (filterType && filterType.trim() !== "") {
    filter += "`$.spec.type` ='" + filterType.toLowerCase().trim() + "' ";
  }
  return filter;
};

const ALL_ADDRESS_SPACES_SORT = (sortBy?: ISortBy) => {
  let orderBy = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 1:
        orderBy = "`$.metadata.name` ";
        break;
      case 2:
      case 3:
        break;
      case 4:
        orderBy = "`$.metadata.creationTimestamp` ";
        break;
      default:
        break;
    }
    if (sortBy.direction) {
      orderBy += sortBy.direction;
    }
  }
  return orderBy;
};

const RETURN_ALL_ADDRESS_SPACES = (
  page: number,
  perPage: number,
  filterNames?: any[],
  filterNameSpaces?: any[],
  filterType?: string | null,
  sortBy?: ISortBy
) => {
  let filter = ALL_ADDRESS_SPACES_FILTER(
    filterNames,
    filterNameSpaces,
    filterType
  );
  let orderBy = ALL_ADDRESS_SPACES_SORT(sortBy);

  const ALL_ADDRESS_SPACES = gql`
      query all_address_spaces {
        addressSpaces(filter: "${filter}"  
        first:${perPage} offset:${perPage * (page - 1)} orderBy:"${orderBy}") {
          total
          addressSpaces {
            metadata {
              namespace
              name
              creationTimestamp
            }
            spec {
              type
              plan {
                metadata {
                  name
                }
                spec {
                  displayName
                }
              }
              authenticationService{
                name
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

const RETURN_ADDRESS_SPACE_DETAIL = (name?: string, namespace?: string) => {
  const ADDRESS_SPACE_DETAIL = gql`
      query all_address_spaces {
        addressSpaces(
          filter: "\`$.metadata.name\` = '${name}' AND \`$.metadata.namespace\` = '${namespace}'"
        ) {
          addressSpaces {
            metadata {
              namespace
              name
              creationTimestamp
            }
            spec {
              type
              plan {
                metadata {
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

const CREATE_ADDRESS_SPACE = gql`
  mutation create_as($as: AddressSpace_enmasse_io_v1beta1_Input!) {
    createAddressSpace(input: $as) {
      name
      uid
      creationTimestamp
    }
  }
`;

const EDIT_ADDRESS_SPACE = gql`
  mutation patch_as(
    $a: ObjectMeta_v1_Input!
    $jsonPatch: String!
    $patchType: String!
  ) {
    patchAddressSpace(input: $a, jsonPatch: $jsonPatch, patchType: $patchType)
  }
`;

const ADDRESS_SPACE_COMMAND_REVIEW_DETAIL = gql`
  query cmd($as: AddressSpace_enmasse_io_v1beta1_Input!) {
    addressSpaceCommand(input: $as)
  }
`;

const RETURN_ALL_ADDRESS_SPACES_FOR_NAME_OR_NAMESPACE = (
  isName: boolean,
  value: string
) => {
  let filter = "";
  if (value) {
    if (isName) {
      filter += "`$.metadata.name` LIKE '" + value + "%'";
    } else {
      filter += "`$.metadata.namespace` LIKE '" + value + "%'";
    }
  }
  const all_address_spaces = gql`
    query all_address_spaces {
      addressSpaces(filter: "${filter}"  
      first:100 offset:0) {
        total
        addressSpaces {
          metadata {
            namespace
            name
          }
        }
      }
    }
    `;
  return all_address_spaces;
};

export {
  DELETE_ADDRESS_SPACE,
  RETURN_ALL_ADDRESS_SPACES,
  RETURN_ADDRESS_SPACE_DETAIL,
  CREATE_ADDRESS_SPACE,
  EDIT_ADDRESS_SPACE,
  ADDRESS_SPACE_COMMAND_REVIEW_DETAIL,
  RETURN_ALL_ADDRESS_SPACES_FOR_NAME_OR_NAMESPACE
};
