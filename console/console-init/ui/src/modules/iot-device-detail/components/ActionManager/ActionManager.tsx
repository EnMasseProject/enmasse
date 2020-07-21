/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useParams } from "react-router";
import { Title, Divider } from "@patternfly/react-core";
import {
  EditMetadataContainer,
  EditDeviceInJsonContainer,
  EditGatewaysContainer,
  EditCredentialsContainer,
  AddCredentialsContainer,
  AddGatewaysContainer
} from "modules/iot-device/containers";
import { DeviceActionType } from "modules/iot-device-detail/utils";

export interface IActionManagerProps {
  actionType: DeviceActionType;
  viaGateway?: boolean;
}

export const ActionManager: React.FC<IActionManagerProps> = ({
  actionType,
  viaGateway
}) => {
  const { deviceid } = useParams();

  const renderComponent = () => {
    switch (actionType) {
      case DeviceActionType.EDIT_METADATA:
        return <EditMetadataContainer />;
      case DeviceActionType.EDIT_DEVICE_IN_JSON:
        return <EditDeviceInJsonContainer />;
      case DeviceActionType.EDIT_GATEWAYS:
        return <EditGatewaysContainer />;
      case DeviceActionType.EDIT_CREDENTIALS:
        return <EditCredentialsContainer />;
      case DeviceActionType.ADD_CREDENTIALS:
        return <AddCredentialsContainer />;
      case DeviceActionType.ADD_GATEWAYS:
        return <AddGatewaysContainer />;
      case DeviceActionType.CHANGE_CONNECTION_TYPE:
        if (viaGateway) {
          return <AddCredentialsContainer />;
        }
        return <AddGatewaysContainer />;
      default:
        return null;
    }
  };

  return (
    <>
      <Title
        headingLevel="h2"
        size="xl"
        id="action-manager-edit-device-title"
        aria-label="Edit device Title"
      >
        Edit device {deviceid}
      </Title>
      <br />
      <Divider />
      <br />
      <br />
      {renderComponent()}
    </>
  );
};
