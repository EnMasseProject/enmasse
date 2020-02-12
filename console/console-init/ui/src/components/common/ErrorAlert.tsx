import React, { useState } from "react";
import { ApolloError } from "apollo-boost";
import { Alert, AlertActionCloseButton } from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";

export interface IError {
  error: ApolloError;
}

export const ErrorAlert: React.FunctionComponent<IError> = ({ error }) => {
  const [alertVisible, setAlertVisible] = useState(true);
  const hideAlert = () => {
    setAlertVisible(false);
  };
  const styles = StyleSheet.create({
    alert_dimentions: {
      width: 40
    }
  });
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
