/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Title,
  EmptyState,
  EmptyStateIcon,
  EmptyStateBody,
  EmptyStateVariant,
  EmptyStateSecondaryActions
} from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { CreateDeviceButton } from "modules/device/components";

export interface IEmptyDeviceListProps {
  handleInputDeviceInfo: () => void;
  handleJSONUpload: () => void;
}

export const EmptyDeviceList: React.FunctionComponent<IEmptyDeviceListProps> = ({
  handleInputDeviceInfo,
  handleJSONUpload
}) => {
  return (
    <EmptyState variant={EmptyStateVariant.full} id="empty-device">
      <EmptyStateIcon icon={PlusCircleIcon} />
      <Title id="empty-device-title" size="lg">
        No Devices
      </Title>
      <EmptyStateBody id="empty-device-body">
        You don't have any devices here. Add devices to your <br />
        IoT Project to continue.
      </EmptyStateBody>
      <EmptyStateSecondaryActions>
        <CreateDeviceButton
          handleInputDeviceInfo={handleInputDeviceInfo}
          handleJSONUpload={handleJSONUpload}
        />
      </EmptyStateSecondaryActions>
    </EmptyState>
  );
};
