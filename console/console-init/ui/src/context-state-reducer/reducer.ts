/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { types } from "./actions";
import {
  initialState,
  initialStateModal,
  initialDeviceState
} from "./initialState";
import { combineReducers } from "./combineReducers";

export interface IActionType {
  type: string;
  payload?: any;
  hasServerError?: boolean;
  hasNetworkError?: boolean;
}

export interface IModalActionType {
  type: string;
  modalType: string;
  modalProps: any;
}

interface IDeviceActionType {
  payload?: any;
  type: string;
}

const errorReducer = (state = initialState, action: IActionType) => {
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

const modalReducer = (state = initialStateModal, action: IModalActionType) => {
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

const deviceReducer = (
  state = initialDeviceState,
  action: IDeviceActionType
) => {
  const { actionType } = action?.payload || {};
  switch (action.type) {
    case types.SET_DEVICE_ACTION_TYPE:
      return {
        ...state,
        actionType: actionType
      };
    case types.RESET_DEVICE_ACTION_TYPE:
      return initialState;
    default:
      return state;
  }
};

export const rootReducer = combineReducers({
  error: errorReducer,
  modal: modalReducer,
  device: deviceReducer
});
