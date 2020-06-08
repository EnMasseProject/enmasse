/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useParams } from "react-router";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_IOT_DEVICE_DETAIL } from "graphql-module/queries";
import { IDeviceDetailResponse } from "schema";
import { DeviceInfo } from "modules/iot-device-detail/components";

export interface IDeviceInfoContainerProps {
  id: string;
}

export const DeviceInfoContainer: React.FC<IDeviceInfoContainerProps> = ({
  id
}) => {
  const { projectname, deviceid } = useParams();

  const { data } = useQuery<IDeviceDetailResponse>(
    RETURN_IOT_DEVICE_DETAIL(projectname, deviceid)
  );

  const { credentials, jsonData } = data?.devices?.devices[0] || {};
  const credentialsJson = credentials && JSON.parse(credentials);
  const deviceJson = jsonData && JSON.parse(jsonData);

  const metadetaJson = {
    default: deviceJson?.default,
    ext: deviceJson?.ext
  };

  return (
    <DeviceInfo
      id={id}
      deviceList={deviceJson?.via}
      metadataList={metadetaJson}
      credentials={credentialsJson}
    />
  );
};
