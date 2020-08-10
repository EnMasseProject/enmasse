/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Alert, AlertActionLink, Flex, FlexItem } from "@patternfly/react-core";
import { useStoreContext, types } from "context-state-reducer";
import { DeviceActionType } from "modules/iot-device-detail/utils";

export interface IErrorStateAlertProps {
  errorState?: string;
  deleteGateways?: () => void;
  deleteCredentials?: () => void;
}

export enum ErrorState {
  CONFLICTING = "conflicting",
  MISSING = "missing"
}

export const ErrorStateAlert: React.FC<IErrorStateAlertProps> = ({
  errorState,
  deleteGateways,
  deleteCredentials
}) => {
  const { dispatch } = useStoreContext();

  const getTitle = () => {
    let title =
      errorState === ErrorState.CONFLICTING
        ? "Opposing connection information"
        : "Missing gateways or credentials";
    return title;
  };

  const getAlertMessage = () => {
    if (errorState === ErrorState.MISSING) {
      return (
        <>
          To enable a device to connect, it needs direct connection credentials
          or alt least one gateway.
        </>
      );
    } else if (ErrorState.CONFLICTING) {
      return (
        <>
          This device has two opposing types of connection information. Would
          you like to remove one connection?
        </>
      );
    }
  };

  const addGateways = () => {
    dispatch({
      type: types.SET_DEVICE_ACTION_TYPE,
      payload: { actionType: DeviceActionType.ADD_GATEWAYS }
    });
  };

  const addCredentials = () => {
    dispatch({
      type: types.SET_DEVICE_ACTION_TYPE,
      payload: { actionType: DeviceActionType.ADD_CREDENTIALS }
    });
  };

  const getActionLinks = () => {
    if (errorState === ErrorState.MISSING) {
      return (
        <Flex>
          <FlexItem>
            <AlertActionLink onClick={addGateways}>
              Add gateway assignment
            </AlertActionLink>
          </FlexItem>
          <FlexItem>or</FlexItem>
          <FlexItem>
            <AlertActionLink
              id="error-state-add-credentials-actionlink"
              onClick={addCredentials}
            >
              Add credentials
            </AlertActionLink>
          </FlexItem>
        </Flex>
      );
    } else if (ErrorState.CONFLICTING) {
      return (
        <Flex>
          <FlexItem>
            <AlertActionLink
              id="error-state-delete-gateways-actionlink"
              onClick={deleteGateways}
            >
              Remove gateways assignment
            </AlertActionLink>
          </FlexItem>
          <FlexItem>or</FlexItem>
          <FlexItem>
            <AlertActionLink
              id="error-state-delete-credentials-actionlink"
              onClick={deleteCredentials}
            >
              Remove credentials
            </AlertActionLink>
          </FlexItem>
        </Flex>
      );
    }
  };

  return (
    <Alert
      variant="warning"
      isInline
      title={getTitle()}
      actionLinks={getActionLinks()}
    >
      {getAlertMessage()}
    </Alert>
  );
};
