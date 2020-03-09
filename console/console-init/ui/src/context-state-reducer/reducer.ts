/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { types } from "./actions";
import { initialState, initialStateModal } from "./initialState";
import { combineReducers } from "./combineReducers";

export interface IActionType {
  type: string;
  payload?: any;
  hasServerError?: boolean;
  hasNetworkError?: boolean;
}

export const errorReducer = (state = initialState, action: IActionType) => {
  const { errors, statusCode } = action.payload || {};
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
        hasNetworkError: true,
        statusCode
      };
    default:
      return state;
  }
};

export interface IModalActionType {
  type: string;
  modalType: string;
  modalProps: any;
}

export const modalReducer = (
  state = initialStateModal,
  action: IModalActionType
) => {
  switch (action.type) {
    case types.SHOW_MODAL:
      return {
        modalType: action.modalType,
        modalProps: action.modalProps
      };
    case types.HIDE_MODAL:
      return initialStateModal;
    default:
      return state;
  }
};

export const rootReducer = combineReducers({
  error: errorReducer,
  modal: modalReducer
});
