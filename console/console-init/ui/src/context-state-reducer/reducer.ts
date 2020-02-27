/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { types } from "./actions";
import { initialState } from "./initialState";

export interface IActionType {
  type: string;
  payload?: any;
  hasServerError?: boolean;
  hasNetworkError?: boolean;
}

export const reducer = (state = initialState, action: IActionType) => {
  const { errors } = action.payload || {};
  switch (action.type) {
    case types.SET_SERVER_ERROR:
      return {
        ...state,
        hasServerError: true,
        errors
      };
    case types.RESET_SERVER_ERROR:
    case types.RESET_NETWORK_ERROR:
      return initialState;
    case types.SET_NETWORK_ERROR:
      return {
        ...state,
        hasNetworkError: action.payload
      };
    default:
      return state;
  }
};
