/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { types } from "context-state-reducer";

const onServerError = (
  error: any,
  history: any,
  dispactAction: any,
  hasServerError: boolean
) => {
  const { graphQLErrors, networkError } = error;
  if (networkError && !graphQLErrors) {
    history && history.push("/server-error");
  } else if (graphQLErrors) {
    hasServerError !== true &&
      dispactAction &&
      dispactAction({
        type: types.SET_SERVER_ERROR,
        payload: { errors: [error] }
      });
  }
};

export { onServerError };
