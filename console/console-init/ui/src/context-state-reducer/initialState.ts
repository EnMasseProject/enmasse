/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

export interface IInitialState {
  error: {
    hasServerError: boolean;
    hasNetworkError: boolean;
    errors: any[];
  };
}

export const initialState: IInitialState = {
  error: {
    hasServerError: false,
    hasNetworkError: false,
    errors: []
  }
};
export interface IInitialStateModal {
  modal: {
    modalType: string | null;
    modalProps: any;
  };
}

export const initialStateModal: IInitialStateModal = {
  modal: {
    modalType: null,
    modalProps: {}
  }
};
