/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { IoTProjectDetailHeader } from "modules/iot-project-detail/components";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_IOT_PROJECTS } from "graphql-module/queries/iot_project";
import { IIoTProjectsResponse } from "schema/iot_project";

interface IIoTProjectDetailHeaderContainerProps {
  projectName: string;
}

export const IoTProjectDetailHeaderContainer: React.FC<IIoTProjectDetailHeaderContainerProps> = ({
  projectName
}) => {
  const { data } = useQuery<IIoTProjectsResponse>(
    RETURN_IOT_PROJECTS({ projectName })
  );

  const { allProjects } = data || {
    allProjects: { iotProjects: [] }
  };

  const { spec, metadata, status, enabled } =
    allProjects?.iotProjects?.[0] || {};

  // TODO: HANDLE AFTER MOCK IS READY
  const handleDelete = () => {};

  // TODO: HANDLE AFTER MOCK IS READY
  const handleChangeEnabled = () => {};

  return (
    <IoTProjectDetailHeader
      projectName={metadata?.name}
      type={spec?.downstreamStrategyType}
      status={status?.phase}
      isEnabled={enabled}
      changeEnable={handleChangeEnabled}
      onDelete={handleDelete}
    />
  );
};
