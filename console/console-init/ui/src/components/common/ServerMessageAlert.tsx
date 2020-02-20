import React, { useState, useEffect } from "react";
import { Alert, AlertActionCloseButton } from "@patternfly/react-core";

import { useErrorContext, types } from "context-state-reducer";

export const ServerMessageAlert: React.FC = () => {
  const { state, dispatch } = useErrorContext();
  const { hasServerError, errors } = state;
  const [alertVisible, setAlertVisible] = useState(true);

  const onClose = () => {
    setAlertVisible(false);
    dispatch({ type: types.RESET_SERVER_ERROR });
  };

  const getErrorMessage = () => {
    const { graphQLErrors } = errors;
    let message: string = "Something went wrong, please try again...";
    if (graphQLErrors && graphQLErrors.length > 0) {
      message = graphQLErrors[0].message;
    }
    return message;
  };

  useEffect(() => {
    hasServerError !== undefined && setAlertVisible(hasServerError);
  }, [hasServerError]);

  if (hasServerError && alertVisible) {
    return (
      <Alert
        variant="danger"
        title="Server Error"
        action={<AlertActionCloseButton onClose={onClose} />}
      >
        {getErrorMessage()}
      </Alert>
    );
  }
  return null;
};
