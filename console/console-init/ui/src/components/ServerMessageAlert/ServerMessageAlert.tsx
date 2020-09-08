/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  Alert,
  AlertActionCloseButton,
  List,
  ListItem,
  AlertGroup,
  AlertVariant
} from "@patternfly/react-core";
import { useStoreContext } from "context-state-reducer";
import { uniqueId } from "utils";
import { ActionStatus } from "constant";

interface IAlert {
  key?: string;
  query?: string;
  title?: string;
  variant?: AlertVariant;
  message?: any;
}

export const ServerMessageAlert: React.FC = () => {
  const { state } = useStoreContext();
  const { errors, status, message, title } = state && state.error;
  const [alerts, setAlerts] = useState<IAlert[]>([]);

  const getErrorMessage = () => {
    let messages: string[] = [];
    if (errors && Array.isArray(errors)) {
      return (
        <List id="serever-message-alert-list">
          {errors &&
            errors.map((error: any, index: number) => {
              const { networkError, graphQLErrors } = error;
              if (graphQLErrors && graphQLErrors.length > 0) {
                graphQLErrors.forEach((err: any) => {
                  messages.push(err.message);
                });
              } else {
                if (
                  networkError &&
                  networkError.result &&
                  networkError.result.errors &&
                  networkError.result.errors.length > 0
                ) {
                  networkError.result.errors.forEach((err: any) =>
                    messages.push(err.message)
                  );
                }
              }
              return messages.map((message: string) => (
                <ListItem key={`li-error-${index}`}>{message}</ListItem>
              ));
            })}
        </List>
      );
    } else {
      messages.push("Something went wrong, please try again...");
    }
    return messages[0];
  };

  const getQueryName = () => {
    return Array.isArray(errors) && errors[0]?.operation?.operationName;
  };

  const getQueryType = () => {
    return (
      Array.isArray(errors) &&
      errors[0]?.operation?.query.definitions[0].operation
    );
  };

  const getAlertDetail = () => {
    if (status === ActionStatus.Failed) {
      return {
        key: uniqueId(),
        query: getQueryName(),
        title: title || "Server error",
        variant: AlertVariant.danger,
        message: message || getErrorMessage()
      };
    } else if (status === ActionStatus.Success) {
      return {
        key: uniqueId(),
        query: getQueryName(),
        title: title || "Action has completed successfuly",
        variant: AlertVariant.success,
        message
      };
    }
  };

  const shouldVisibleAlert = () => {
    const query = getQueryName();
    const alertIndex = alerts?.findIndex(
      (alert: IAlert) => alert?.query === query
    );
    if (getQueryType() === "query" && alertIndex > -1) {
      return false;
    }
    return true;
  };

  const removeAlert = (key: string | undefined) => {
    const newAlerts = alerts?.filter((alert: IAlert) => alert?.key !== key);
    setAlerts(newAlerts);
  };

  useEffect(() => {
    if (errors && shouldVisibleAlert()) {
      setAlerts([...alerts, { ...getAlertDetail() }]);
    }
  }, [errors]);

  return (
    <AlertGroup isToast>
      {alerts?.map((alert: IAlert) => {
        const { key, variant, title, message } = alert;
        return (
          <Alert
            id="server-message-alert"
            isLiveRegion
            variant={variant}
            title={title}
            actionClose={
              <AlertActionCloseButton
                title={title}
                variantLabel={`${variant} alert`}
                onClose={() => removeAlert(key)}
              />
            }
            key={key}
          >
            {message}
          </Alert>
        );
      })}
    </AlertGroup>
  );
};
