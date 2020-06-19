/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Alert, Button, ButtonVariant } from "@patternfly/react-core";

// const styles = StyleSheet.create({
//   alert_background_color: {
//     backgroundColor: "#fdf7e7"
//   }
// });

export interface IErrorStateAlertProps {
  errorState?: string;
  addGateways?: () => void;
  addCredentials?: () => void;
  deleteGateways?: () => void;
  deleteCredentials?: () => void;
}

export enum ErrorState {
  CONFLICTING = "conflicting",
  MISSING = "missing"
}

export const ErrorStateAlert: React.FC<IErrorStateAlertProps> = ({
  errorState,
  addGateways,
  addCredentials,
  deleteGateways,
  deleteCredentials
}) => {
  const getTitle = () => {
    let title =
      errorState === ErrorState.CONFLICTING
        ? "Conflicting connection types"
        : "Missing gateways or credentials";
    return title;
  };

  const getAlertBody = () => {
    if (errorState === ErrorState.MISSING) {
      return (
        <>
          In order to enable a device to connect, it either needs credentials or
          gateways/a gateway.
          <br />
          <Button variant={ButtonVariant.link} onClick={addGateways}>
            Add gateways
          </Button>{" "}
          or
          <Button variant={ButtonVariant.link} onClick={addCredentials}>
            Add credentials
          </Button>
        </>
      );
    } else if (ErrorState.CONFLICTING) {
      return (
        <>
          This device has two connection types that are conflicting with each
          other. You can choose actions to fix this issue.
          <br />
          <Button
            id="es-delete-gateway-button"
            variant={ButtonVariant.link}
            onClick={deleteGateways}
          >
            Delete gateways
          </Button>{" "}
          or
          <Button
            id="es-delete-credentials-button"
            variant={ButtonVariant.link}
            onClick={deleteCredentials}
          >
            Delete credentials
          </Button>
        </>
      );
    }
  };

  return (
    <Alert
      variant="warning"
      isInline
      title={getTitle()}
      // className={styles.alert_background_color}
    >
      {getAlertBody()}
    </Alert>
  );
};
