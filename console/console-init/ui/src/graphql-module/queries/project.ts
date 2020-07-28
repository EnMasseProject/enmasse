/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { IProjectFilter } from "modules/project/ProjectPage";
import { generateFilterPattern } from "./query";
import { removeForbiddenChars } from "utils";
import { ISortBy } from "@patternfly/react-table";

const FILTER_PROJECTS = (projectFilterParams: IProjectFilter) => {
  const { names, namespaces, type, status } = projectFilterParams;
  let filter: string = "";
  let namesLength = names?.length;
  let namespacesLength = namespaces?.length;

  //filter names
  filter += generateFilterPattern("metadata.name", names);

  if (namesLength && namespacesLength && namespacesLength > 0) {
    filter += " AND ";
  }

  //filter namespaces
  filter += generateFilterPattern("metadata.namespace", namespaces);

  if (
    ((namesLength && namesLength > 0) ||
      (namespacesLength && namespacesLength > 0)) &&
    type?.value?.trim()
  ) {
    filter += " AND ";
  }

  //filter type
  if (type && type.value.trim() !== "") {
    filter += "`$.kind`= '" + type.value + "'";
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
        // add object to fetch all projects whose status.phase is empty string
        // as initially when the project is created the phase var is initialised as empty string
        { value: "", isExact: true }
      ]);
    }
  }
  return filter;
};

const ALL_PROJECTS_SORT = (sortBy?: ISortBy) => {
  let orderBy = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 1:
        orderBy = "`$.metadata.name` ";
        break;
      case 2:
        orderBy = "`$.kind` ";
        break;
      case 3:
        orderBy = "`$.status.phase` ";
        break;
      case 4:
        orderBy = "`$.metadata.creationTimestamp` ";
        break;
      default:
        break;
    }
    if (orderBy.trim() != "" && sortBy.direction) {
      orderBy += sortBy.direction;
    }
  }
  return orderBy;
};

const RETURN_ALL_PROJECTS = (
  page: number,
  perPage: number,
  projectFilterParams?: any,
  queryResolver?: string,
  sortBy?: ISortBy
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
          addresses{
						total
          }
          connections{
            total
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
    filter = FILTER_PROJECTS(projectFilterParams);
  }

  let orderBy = ALL_PROJECTS_SORT(sortBy);

  const ALL_PROJECTS = gql`
    query allProjects {
        allProjects(
          filter: "${filter}"
        first:${perPage} offset:${perPage * (page - 1)} orderBy:"${orderBy}"
        ) {
           ${queryResolver}
        }
      }
  `;

  return ALL_PROJECTS;
};

const RETURN_ALL_PROJECTS_FOR_NAME_OR_NAMESPACE = (
  propertyName: string,
  value: string
) => {
  let filter = "";
  value = removeForbiddenChars(value);
  if (value && propertyName) {
    filter += "`$.metadata." + [propertyName] + "` LIKE '" + value + "%'";
  }

  const all_proejcts = gql(`
    query all_projects {
      allProjects(filter: "${filter}" first:100 offset:0) {
      total
      objects{
        ... on AddressSpace_consoleapi_enmasse_io_v1beta1 {
          kind
          metadata {
            name
            namespace
            creationTimestamp
          }
        }
        ... on IoTProject_iot_enmasse_io_v1alpha1 {
          kind
          metadata {
            name
            namespace
            creationTimestamp
          }
        }
      }
     }
    }
    `);
  return all_proejcts;
};

export { RETURN_ALL_PROJECTS, RETURN_ALL_PROJECTS_FOR_NAME_OR_NAMESPACE };
