/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import { useHistory } from "react-router-dom";
import { StyleSheet, css } from "aphrodite";
import {
  Alert,
  PageSection,
  AlertActionCloseButton,
  AlertActionLink
} from "@patternfly/react-core";

import { useStoreContext, types } from "context-state-reducer";
import { ErrorCodes } from "constant";

const styles = StyleSheet.create({
  alert: {
    backgroundColor: "var(--pf-c-alert--m-inline--BackgroundColor)"
  }
});

const NetworkStatusAlert: React.FunctionComponent = () => {
  const { state, dispatch } = useStoreContext();
  const { hasNetworkError, statusCode } = state && state.error;
  const [alertVisible, setAlertVisible] = useState(true);
  const history = useHistory();

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

  const handleAlertActionLink = () => {
    history.push(redirectLink);
  };

  if (alertVisible && hasNetworkError) {
    return (
      <PageSection>
        <Alert
          id="network-status-disconnected-alert"
          aria-label="Disconnected from server"
          variant="danger"
          title="Disconnected from server"
          actionClose={<AlertActionCloseButton onClose={onClose} />}
          className={css(styles.alert)}
          actionLinks={
            <AlertActionLink
              id="network-status-redirect-text-alert-action-link"
              onClick={handleAlertActionLink}
            >
              {redirectText}
            </AlertActionLink>
          }
        >
          <span>{errorMessage}</span>
        </Alert>
      </PageSection>
    );
  }
  return null;
};

export { NetworkStatusAlert };
