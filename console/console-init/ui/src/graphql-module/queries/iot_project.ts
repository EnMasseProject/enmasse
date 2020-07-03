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

const RETURN_IOT_PROJECTS = (filterObj?: any, queryResolver?: string) => {
  // TODO: Default resolver is subjected to change, with respect to most used query
  const defaultQueryResolver = `
    total
    iotProjects {
      metadata {
        name
        namespace
      }
      enabled
      spec {
        downstreamStrategyType
      }
      status {
        phase
      }
    }
  `;

  if (!queryResolver) {
    queryResolver = defaultQueryResolver;
  }

  let filter: string = "";

  if(filterObj){
    filter = FILTER_RETURN_IOT_PROJECTS(filterObj);
  }

  const IOT_PROJECT_DETAIL = gql(
    `query allProjects {
      allProjects(
        filter: "${filter}",
        projectType: iotProject
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

const ENABLE_IOT_PROJECTS = gql(
  `mutation enable_iotProjects($a:[ObjectMeta_v1_Input!]!) {
    enableIotProjects(input:$a)
  }`
);

const DISABLE_IOT_PROJECTS = gql(
  `mutation disable_iotProjects($a:[ObjectMeta_v1_Input!]!) {
    disableIotProjects(input:$a)
  }`
)

export { RETURN_IOT_PROJECTS, DELETE_IOT_PROJECT, ENABLE_IOT_PROJECTS, DISABLE_IOT_PROJECTS };
