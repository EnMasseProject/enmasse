/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  Alert,
  PageSection,
  AlertActionCloseButton
} from "@patternfly/react-core";

import { useErrorContext, types } from "context-state-reducer";

const NetworkStatusAlert: React.FunctionComponent = () => {
  const { state, dispatch } = useErrorContext();
  const { hasNetworkError, statusCode } = state;
  const [alertVisible, setAlertVisible] = useState(true);

  const onClose = () => {
    setAlertVisible(false);
    dispatch({ type: types.RESET_NETWORK_ERROR });
  };

  useEffect(() => {
    hasNetworkError !== undefined && setAlertVisible(hasNetworkError);
  }, [hasNetworkError]);

  const errorMessage =
    statusCode && statusCode === 403
      ? "You're disconnected from the server, please sign in again."
      : "We're having trouble connecting to our server.";

  if (alertVisible && hasNetworkError) {
    return (
      <PageSection>
        <Alert
          variant="danger"
          title="Disconnected from server"
          action={<AlertActionCloseButton onClose={onClose} />}
        >
          <span>{errorMessage}</span>
        </Alert>
        <br />
        <a href="oauth/sign_in" className="pf-c-nav__link">
          Take me home
        </a>
      </PageSection>
    );
  }
  return null;
};

export { NetworkStatusAlert };
