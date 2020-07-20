/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";

const FILTER_RETURN_IOT_PROJECTS = (filterObject?: any) => {
  const { projectName } = filterObject;
  let filter: string = "";
  if (projectName && projectName.trim() !== "") {
    filter += "`$.metadata.name` = '" + projectName + "'";
  }

  // TODO: Filters to be incrementally added
  return filter;
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

  let filter: string = "`$.kind`='IoTProject'";

  if (filterObj) {
    filter += " AND ";
    filter += FILTER_RETURN_IOT_PROJECTS(filterObj);
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
  `  mutation delete_iot_project($a: [ObjectMeta_v1_Input!]!) {
    deleteIotProjects(input: $a)
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

const IOT_PROJECT_COMMAND_REVIEW_DETAIL = gql`
  query iotProjectCommand(
    $iotProject: IotProject_iot_enmasse_io_v1alpha1_input!
  ) {
    iotProjectCommand(input: $iotProject)
  }
`;

export {
  RETURN_IOT_PROJECTS,
  DELETE_IOT_PROJECT,
  TOGGLE_IOT_PROJECTS_STATUS,
  IOT_PROJECT_COMMAND_REVIEW_DETAIL
};
