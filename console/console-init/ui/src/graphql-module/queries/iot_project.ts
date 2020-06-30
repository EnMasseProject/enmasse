/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";

const FILTER_RETURN_IOT_PROJECTS = (filterObject: any) => {
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
    iotProjects {
      metadata {
        name
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

  let filter = FILTER_RETURN_IOT_PROJECTS(filterObj);

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

export { RETURN_IOT_PROJECTS };
