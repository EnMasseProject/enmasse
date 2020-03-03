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
import { ErrorCodes } from "constants/constants";

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

  let errorMessage = "We're having trouble connecting to our server.";
  let redirectLink = "/";
  let redirectText = "Take me Home";

  if (statusCode && statusCode === ErrorCodes.FORBIDDEN) {
    errorMessage = "Your session has expired. Please login again.";
    redirectLink = "oauth/sign_in";
    redirectText = "Sign in";
  }

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
        <a href={redirectLink} className="pf-c-nav__link">
          {redirectText}
        </a>
      </PageSection>
    );
  }
  return null;
};

export { NetworkStatusAlert };
