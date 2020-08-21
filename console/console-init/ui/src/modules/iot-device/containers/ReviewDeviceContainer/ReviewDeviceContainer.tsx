/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React from "react";
import { ReviewDevice } from "modules/iot-device/components/ReviewDevice";
import { ICredentialView } from "modules/iot-device-detail/components";

interface IReviewDeviceContainerProps {
  device?: IDeviceProp;
  title?: string;
}

export interface IDeviceProp {
  deviceInformation: {
    deviceId?: string;
    status?: boolean;
    metadata?: any[];
  };
  connectionType: string;
  gateways: {
    gateways?: string[];
    gatewayGroups?: string[];
  };
  memberOf?: string[];
  credentials?: ICredentialView[];
}

const ReviewDeviceContainer: React.FunctionComponent<IReviewDeviceContainerProps> = ({
  title,
  device
}) => {
  return (
    <>
      <ReviewDevice device={device} title={title} />
    </>
  );
};

export { ReviewDeviceContainer };
