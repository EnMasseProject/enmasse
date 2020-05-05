/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { ISortBy } from "@patternfly/react-table";
import { removeForbiddenChars } from "utils";
import { generateFilterPattern } from "./query";

const DELETE_ADDRESS_SPACE = gql`
  mutation delete_as($a: ObjectMeta_v1_Input!) {
    deleteAddressSpace(input: $a)
  }
`;

const ALL_ADDRESS_SPACES_FILTER = (
  filterNames?: any[],
  filterNameSpaces?: any[],
  filterType?: string | null,
  filterStatus?: string | null
) => {
  let filter = "";
  let filterNamesLength = filterNames && filterNames.length;
  let filterNameSpacesLength = filterNameSpaces && filterNameSpaces.length;
  //filter names
  filter += generateFilterPattern("metadata.name", filterNames);

  if (
    filterNamesLength &&
    filterNameSpacesLength &&
    filterNameSpacesLength > 0
  ) {
    filter += " AND ";
  }

  //filter namsespaces
  filter += generateFilterPattern("metadata.namespace", filterNameSpaces);

  if (
    ((filterNamesLength && filterNamesLength > 0) ||
      (filterNameSpacesLength && filterNameSpacesLength > 0)) &&
    filterType &&
    filterType.trim() !== ""
  ) {
    filter += " AND ";
  }

  //filter tye
  if (filterType) {
    filter += generateFilterPattern("spec.type", [
      { value: filterType.toLowerCase(), isExact: true }
    ]);
  }

  //filter tye
  if (filterStatus) {
    filter += generateFilterPattern("status.phase", [
      { value: filterStatus, isExact: true }
    ]);
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
  filterStatus?: string | null,
  sortBy?: ISortBy
) => {
  let filter = ALL_ADDRESS_SPACES_FILTER(
    filterNames,
    filterNameSpaces,
    filterType,
    filterStatus
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
  propertyName: string,
  value: string
) => {
  let filter = "";
  value = removeForbiddenChars(value);
  if (value && propertyName) {
    filter += "`$.metadata." + [propertyName] + "` LIKE '" + value + "%'";
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
