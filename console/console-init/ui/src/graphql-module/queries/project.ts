/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";

const FILTER_RETURN_PROJECTS = (filterObject?: any) => {
  const { projectname, projectType } = filterObject;
  let filter: string = "";
  if (projectname && projectname.trim() !== "") {
    filter += "`$.metadata.name` = '" + projectname + "'";
  }

  // TODO: Filters to be incrementally added
  return filter;
};

const RETURN_ALL_PROJECTS = (filterObj?: any, queryResolver?: string) => {
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

  if (filterObj) {
    //TODO
    filter = FILTER_RETURN_PROJECTS(filterObj);
  }

  const ALL_PROJECTS = gql(
    `query allProjects {
        allProjects(
          filter: "${filter}",
        ) {
           ${queryResolver}
        }
      }`
  );

  return ALL_PROJECTS;
};

export { RETURN_ALL_PROJECTS };
