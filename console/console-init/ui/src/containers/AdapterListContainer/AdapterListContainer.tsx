/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useParams } from "react-router";
import { useQuery } from "@apollo/react-hooks";
import { AdapterList } from "components";
import { RETURN_IOT_PROJECTS } from "graphql-module/queries";
import { IIoTProjectsResponse } from "schema/iot_project";

export const AdapterListContainer: React.FC<{ id: string }> = ({ id }) => {
  const { projectname } = useParams();

  const queryResolver = `
  iotProjects {
    endpoints{
      name
      url
      host
      port
      tls
    }
  }`;

  const { data } = useQuery<IIoTProjectsResponse>(
    RETURN_IOT_PROJECTS({ projectName: projectname }, queryResolver)
  );

  const { allProjects } = data || {
    allProjects: { iotProjects: [] }
  };
  const adapters = allProjects?.iotProjects?.[0]?.endpoints || [];

  return <AdapterList id={id} adapters={adapters} />;
};
