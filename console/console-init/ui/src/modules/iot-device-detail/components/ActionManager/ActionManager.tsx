/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useParams } from "react-router";
import { EditMetadataContainer } from "modules/iot-device/containers";
import { DeviceActionType } from "modules/iot-device-detail/utils";

export interface IActionManagerProps {
  actionType: DeviceActionType;
}

export const ActionManager: React.FC<IActionManagerProps> = ({
  actionType
}) => {
  const { deviceid } = useParams();

  switch (actionType) {
    case DeviceActionType.EDIT_METADATA:
      return <EditMetadataContainer title={`Edit ${deviceid}`} />;
    default:
      return null;
  }
};
