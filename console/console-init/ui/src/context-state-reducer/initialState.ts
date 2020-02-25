/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

export interface IInitialState {
  hasServerError: boolean;
  hasNetworkError: boolean;
  errors: any[];
}

export const initialState: IInitialState = {
  hasServerError: false,
  hasNetworkError: false,
  errors: []
};
