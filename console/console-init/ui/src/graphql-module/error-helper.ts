/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { types } from "context-state-reducer";
import { QUERY } from "constants/constants";

const onServerError = (error: any, dispactAction: any, states: any) => {
  const { graphQLErrors, networkError, operation } = error;
  const { hasNetworkError, hasServerError } = states;
  const operationType =
    operation &&
    operation.query &&
    operation.query.definitions[0] &&
    operation.query.definitions[0].operation;
  if (networkError && !graphQLErrors) {
    dispactAction &&
      hasNetworkError !== true &&
      dispactAction({
        type: types.SET_NETWORK_ERROR,
        payload: { statusCode: networkError && networkError.statusCode }
      });
  }
  //catch the server error for queries
  else if (graphQLErrors && operationType === QUERY) {
    hasServerError !== true &&
      dispactAction &&
      dispactAction({
        type: types.SET_SERVER_ERROR,
        payload: { errors: [error] }
      });
  }
};

export { onServerError };
