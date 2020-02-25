/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { NavLink, useHistory } from "react-router-dom";
import { Alert, AlertActionCloseButton } from "@patternfly/react-core";

import { useErrorContext, types } from "context-state-reducer";

const NetworkStatusAlert: React.FunctionComponent = () => {
  const { state, dispatch } = useErrorContext();
  const history = useHistory();
  const { hasNetworkError } = state;
  const [alertVisible, setAlertVisible] = useState(true);

  const onCloseAlert = () => {
    setAlertVisible(false);
    dispatch({ type: types.SET_NETWORK_ERROR, payload: false });
  };

  const onClose = () => {
    onCloseAlert();
  };

  const redirectToLogin = () => {
    onCloseAlert();
    history.push("oauth/sign_in");
  };

  if (alertVisible && hasNetworkError) {
    return (
      <Alert
        variant="warning"
        title="Server disconnected..."
        action={<AlertActionCloseButton onClose={onClose} />}
      >
        <a onClick={redirectToLogin} className="pf-c-nav__link">
          Take me home
        </a>
      </Alert>
    );
  }
  return null;
};

export { NetworkStatusAlert };
