/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";

const FILTER_RETURN_IOT_PROJECTS = (filterObject?: any) => {
  const { projectname } = filterObject;
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
    // filter = FILTER_RETURN_IOT_PROJECTS(filterObj);
  }

  const IOT_PROJECT_DETAIL = gql(
    `query allProjects {
      allProjects(
        filter: "${filter}",
      ) {
         ${queryResolver}
      }
    }`
  );

  return IOT_PROJECT_DETAIL;
};

const RETURN_IOT_PROJECTS = (filterObj?: any, queryResolver?: string) => {
  // TODO: Default resolver is subjected to change, with respect to most used query
  const defaultQueryResolver = `
    total
    objects{
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
    FILTER_RETURN_IOT_PROJECTS(filterObj);
  }

  const IOT_PROJECT_DETAIL = gql(
    `query allProjects {
      allProjects(
        filter: "${filter}",
      ) {
         ${queryResolver}
      }
    }`
  );

  return IOT_PROJECT_DETAIL;
};

const DELETE_IOT_PROJECT = gql(
  `  mutation delete_iot_project($as: [ObjectMeta_v1_Input!]!) {
    deleteIotProjects(input: $as)
  }`
);

export const CREATE_IOT_PROJECT = gql`
  mutation createIotProject(
    $project: IotProject_iot_enmasse_io_v1alpha1_input!
  ) {
    createIotProject(input: $project) {
      name
      namespace
    }
  }
`;

const TOGGLE_IOT_PROJECTS_STATUS = gql(
  `mutation toggle_iot_project_status($a: [ObjectMeta_v1_Input!]!, $status: Boolean!){
    toggleIoTProjectsStatus(input: $a, status: $status)
  }`
);

export {
  RETURN_IOT_PROJECTS,
  DELETE_IOT_PROJECT,
  TOGGLE_IOT_PROJECTS_STATUS,
  RETURN_ALL_PROJECTS
};
