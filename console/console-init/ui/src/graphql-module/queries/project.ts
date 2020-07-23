/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { IProjectFilter } from "modules/project/ProjectPage";
import { generateFilterPattern } from "./query";

const FILTER_RETURN_PROJECTS = (projectFilterParams: IProjectFilter) => {
  const { names, namespaces, type, status } = projectFilterParams;
  let filter: string = "";
  let filterNamesLength = names && names.length;
  let filterNameSpacesLength = namespaces && namespaces.length;

  //filter names
  filter += generateFilterPattern("metadata.name", names);

  if (
    filterNamesLength &&
    filterNameSpacesLength &&
    filterNameSpacesLength > 0
  ) {
    filter += " AND ";
  }

  //filter namsespaces
  filter += generateFilterPattern("metadata.namespace", namespaces);

  if (
    ((filterNamesLength && filterNamesLength > 0) ||
      (filterNameSpacesLength && filterNameSpacesLength > 0)) &&
    type?.value?.trim()
  ) {
    filter += " AND ";
  }

  //filter tye
  if (type) {
    if (type && type.value.trim() !== "") {
      filter += "`$.kind`= '" + type.value + "'";
    }
  }

  if (type?.value.trim() && status?.trim()) {
    filter += " AND ";
  }

  //filter status
  if (status) {
    if (status !== "Pending") {
      filter += generateFilterPattern("status.phase", [
        { value: status, isExact: true }
      ]);
    } else {
      filter += generateFilterPattern("status.phase", [
        { value: status, isExact: true },
        { value: "", isExact: true }
      ]);
    }
  }
  return filter;
};

const RETURN_ALL_PROJECTS = (
  projectFilterParams?: any,
  queryResolver?: string
) => {
  // TODO: Default resolver is subjected to change, with respect to most used query
  const defaultQueryResolver = `
      total
      objects{
        ... on AddressSpace_consoleapi_enmasse_io_v1beta1 {
          kind
          metadata {
            name
            namespace
            creationTimestamp
          }
          messagingStatus: status {
            isReady 
            phase
            messages
          }
          spec{
            plan{
              spec{
                displayName
              }
              metadata{
                name
              }
            }
            type
            authenticationService{
              name
            }
          }
        }
        ... on IoTProject_iot_enmasse_io_v1alpha1 {
          kind
          metadata {
            name
            namespace
            creationTimestamp
          }
          iotStatus: status{
            phase
            phaseReason 
          }
          enabled
        }
      }
    `;

  if (!queryResolver) {
    queryResolver = defaultQueryResolver;
  }

  let filter: string = "";

  if (projectFilterParams) {
    filter = FILTER_RETURN_PROJECTS(projectFilterParams);
  }

  const ALL_PROJECTS = gql`
    query allProjects {
        allProjects(
          filter: "${filter}",
        ) {
           ${queryResolver}
        }
      }
  `;

  return ALL_PROJECTS;
};

export { RETURN_ALL_PROJECTS };
