/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useParams } from "react-router";
import { Title, Divider, Text } from "@patternfly/react-core";
import {
  EditMetadataContainer,
  EditDeviceInJsonContainer,
  EditGatewaysContainer,
  EditCredentialsContainer,
  AddCredentialsContainer,
  AddGatewaysContainer
} from "modules/iot-device/containers";
import { DeviceActionType } from "modules/iot-device-detail/utils";
import { ConnectedDirectlyContainer } from "modules/iot-device-detail/containers";
import {
  AddGateways,
  EditGatewayGroupMembershipContainer
} from "modules/iot-device";
import { PageJourney } from "constant";
import { useStoreContext, types } from "context-state-reducer";
import { StyleSheet, css } from "aphrodite";

const styles = StyleSheet.create({
  fontWeight: {
    "font-weight": "var(--pf-global--FontWeight--light)"
  }
});

export interface IActionManagerProps {
  actionType: DeviceActionType;
  viaGateway?: boolean;
}

export const ActionManager: React.FC<IActionManagerProps> = ({
  actionType
}) => {
  const { deviceid } = useParams();
  const { dispatch } = useStoreContext();
  const [pageJourney, setPageJourney] = useState<PageJourney>();

  function renderTitle() {
    switch (pageJourney || actionType) {
      case PageJourney.AddCredential:
        return (
          <Title
            headingLevel="h2"
            size="xl"
            id="edit-device-title"
            aria-label="edit device title"
          >
            Edit credentials
          </Title>
        );
      case PageJourney.AddGatewayGroupMembership:
      case DeviceActionType.EDIT_GATEWAY_GROUP_MEMBERSHIP:
        return (
          <>
            <Title
              id="add-gateway-group-membership-title"
              headingLevel="h1"
              size="xl"
              aria-label="add gatewy group membership title"
            >
              Edit gateway groups membership
              <small className={css(styles.fontWeight)}> (optional)</small>
            </Title>
            <Text>
              If you are adding a gateway device, you can assign it to gateway
              groups
            </Text>
          </>
        );
      default:
        return (
          <Title
            headingLevel="h2"
            size="xl"
            id="edit-device-title"
            aria-label="edit device title"
          >
            Edit device {deviceid}
          </Title>
        );
    }
  }

  const onCancel = () => {
    dispatch({
      type: types.RESET_DEVICE_ACTION_TYPE
    });
  };

  const renderComponent = () => {
    switch (actionType) {
      case DeviceActionType.EDIT_METADATA:
        return <EditMetadataContainer onCancel={onCancel} />;
      case DeviceActionType.EDIT_DEVICE_IN_JSON:
        return <EditDeviceInJsonContainer onCancel={onCancel} />;
      case DeviceActionType.EDIT_GATEWAYS:
        return <EditGatewaysContainer onCancel={onCancel} />;
      case DeviceActionType.EDIT_CREDENTIALS:
        return <EditCredentialsContainer onCancel={onCancel} />;
      case DeviceActionType.ADD_CREDENTIALS:
        return <AddCredentialsContainer onCancel={onCancel} />;
      case DeviceActionType.ADD_GATEWAYS:
        return <AddGatewaysContainer onCancel={onCancel} />;
      case DeviceActionType.CHANGE_CONNECTION_TYPE_CONNECTED_DIRECTLY:
        return (
          <ConnectedDirectlyContainer
            setPageJourney={setPageJourney}
            onCancel={onCancel}
          />
        );
      case DeviceActionType.CHANGE_CONNECTION_TYPE_VIA_GATEWAYS:
        return <AddGateways />;
      case DeviceActionType.EDIT_GATEWAY_GROUP_MEMBERSHIP:
        return <EditGatewayGroupMembershipContainer onCancel={onCancel} />;
      default:
        return null;
    }
  };

  return (
    <>
      {renderTitle()}
      <br />
      <Divider />
      <br />
      <div style={{ marginLeft: "3rem" }}>{renderComponent()}</div>
    </>
  );
};
