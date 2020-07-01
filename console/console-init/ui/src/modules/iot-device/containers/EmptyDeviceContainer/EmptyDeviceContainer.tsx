/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  IEmptyDeviceListProps,
  EmptyDeviceList
} from "modules/iot-device/components";
import { useQuery } from "@apollo/react-hooks";
import { POLL_INTERVAL, FetchPolicy } from "constant";
import { useParams } from "react-router";
import { IIoTDevicesResponse } from "schema/iot_device";
import { RETURN_ALL_DEVICES_FOR_IOT_PROJECT } from "graphql-module/queries";

export interface IEmptyDeviceContainerProps extends IEmptyDeviceListProps {
  setTotalDevices: (val: number) => void;
}

export const EmptyDeviceContainer: React.FC<IEmptyDeviceContainerProps> = ({
  handleInputDeviceInfo,
  handleJSONUpload,
  setTotalDevices
}) => {
  const { projectname } = useParams();

  // To check if the server has atleast one device
  const { data } = useQuery<IIoTDevicesResponse>(
    RETURN_ALL_DEVICES_FOR_IOT_PROJECT(
      1,
      1,
      projectname,
      undefined,
      undefined,
      "total"
    ),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  const total = data?.devices?.total || 0;

  total > 0 && setTotalDevices(total);

  return (
    <EmptyDeviceList
      handleInputDeviceInfo={handleInputDeviceInfo}
      handleJSONUpload={handleJSONUpload}
    />
  );
};
