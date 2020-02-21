/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { ApolloError } from "apollo-boost";
import { Alert, AlertActionCloseButton } from "@patternfly/react-core";

export interface IError {
  error: ApolloError;
}

export const ErrorAlert: React.FunctionComponent<IError> = ({ error }) => {
  const [alertVisible, setAlertVisible] = useState(true);
  const hideAlert = () => {
    setAlertVisible(false);
  };

  return (
    <>
      {alertVisible && (
        <Alert
          variant="danger"
          title={error.message}
          action={<AlertActionCloseButton onClose={hideAlert} />}
        >
          {error.extraInfo}
        </Alert>
      )}
    </>
  );
};
