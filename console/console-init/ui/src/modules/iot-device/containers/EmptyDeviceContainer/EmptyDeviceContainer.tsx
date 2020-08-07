/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useEffect } from "react";
import {
  IEmptyDeviceListProps,
  EmptyDeviceList
} from "modules/iot-device/components";
import { useQuery } from "@apollo/react-hooks";
import { POLL_INTERVAL, FetchPolicy } from "constant";
import { IIoTDevicesResponse } from "schema/iot_device";
import { RETURN_ALL_DEVICES_FOR_IOT_PROJECT } from "graphql-module/queries";
import { StyleSheet, css } from "aphrodite";

const styles = StyleSheet.create({
  min_height: {
    "min-height": "25rem"
  }
});

export interface IEmptyDeviceContainerProps extends IEmptyDeviceListProps {
  setShowEmptyDevice: (val: boolean) => void;
  projectname: string;
  namespace: string;
}

export const EmptyDeviceContainer: React.FC<IEmptyDeviceContainerProps> = ({
  handleInputDeviceInfo,
  handleJSONUpload,
  setShowEmptyDevice,
  projectname,
  namespace
}) => {
  // To check if the server has atleast one device
  const { data } = useQuery<IIoTDevicesResponse>(
    RETURN_ALL_DEVICES_FOR_IOT_PROJECT(
      1,
      1,
      projectname,
      namespace,
      undefined,
      undefined,
      "total"
    ),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  const total = data?.devices?.total || 0;

  useEffect(() => {
    if (total === 0) {
      setShowEmptyDevice(true);
    } else if (total > 0) {
      setShowEmptyDevice(false);
    }
  }, [total]);

  return (
    <>
      {total === 0 && (
        <div className={css(styles.min_height)}>
          <EmptyDeviceList
            handleInputDeviceInfo={handleInputDeviceInfo}
            handleJSONUpload={handleJSONUpload}
          />
        </div>
      )}
    </>
  );
};
