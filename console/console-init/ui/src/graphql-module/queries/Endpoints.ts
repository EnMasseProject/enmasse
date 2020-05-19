/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { ISortBy } from "@patternfly/react-table";
import { generateFilterPattern } from "./query";

const ALL_ENDPOINTS_FOR_ADDRESS_SPACE_FILTER = (
  name?: string,
  namespace?: string
) => {
  let filter = "";

  if (name && name.trim() !== "") {
    filter += "`$.metadata.name` LIKE '" + name + ".%' AND";
  }
  if (namespace && namespace.trim() !== "") {
    filter += "`$.metadata.namespace` = '" + namespace + "'";
  }
  return filter;
};

const RETURN_ALL_ENDPOINTS_FOR_ADDRESS_SPACE = (
  addressSpaceName: string,
  namespace: string,
  page: number,
  perPage: number
) => {
  const filter = ALL_ENDPOINTS_FOR_ADDRESS_SPACE_FILTER(
    addressSpaceName,
    namespace
  );
  //   const orderBy = ALL_ENDPOINTS_SORT(sortBy);
  const all_endpoints = gql`
    query all_endpoints{
      messagingEndpoints( first:${perPage} offset:${perPage * (page - 1)}
        filter:"${filter}")  {
        total
        messagingEndpoints {
          metadata {
            name
            namespace
            creationTimestamp
          }
          spec {
            protocols
          }
          status {
            phase
            type
            message
            host
            ports {
              name
              protocol
              port
            }
            internalPorts {
              name
              protocol
              port
            }
          }
        }
      }
    }
  `;
  return all_endpoints;
};

export { RETURN_ALL_ENDPOINTS_FOR_ADDRESS_SPACE };
