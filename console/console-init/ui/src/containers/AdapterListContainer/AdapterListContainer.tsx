/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useParams } from "react-router";
import { useQuery } from "@apollo/react-hooks";
import { AdapterList, IAdapter } from "components";
import { RETURN_IOT_PROJECTS } from "graphql-module/queries";
import { IIoTProjectsResponse } from "schema/iot_project";

export const AdapterListContainer: React.FC<{ id: string }> = ({ id }) => {
  const { projectname } = useParams();

  const queryResolver = `
  objects {
    ... on IoTProject_iot_enmasse_io_v1alpha1 {
    endpoints{
      name
      url
      host
      port
      tls
    }
  }
  }`;

  const { data } = useQuery<IIoTProjectsResponse>(
    RETURN_IOT_PROJECTS({ projectname }, queryResolver)
  );

  const { allProjects } = data || {
    allProjects: { objects: [] }
  };
  const adapters: IAdapter[] = allProjects.objects[0]?.endpoints || [];

  return <AdapterList id={id} adapters={adapters} />;
};
