/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { createContext, useReducer, useContext, useMemo } from "react";
import { initialState } from "./initialState";
import { reducer } from "./reducer";

const ErrorContext = createContext<any>(initialState);
const ErrorProvider = ErrorContext.Provider;

const useErrorReducer = () => {
  const [state, dispatch] = useReducer(reducer, initialState);
  const contextValue = useMemo(() => {
    return { state, dispatch };
  }, [state, dispatch]);
  return [contextValue];
};

const useErrorContext = () => {
  return useContext(ErrorContext);
};

export { ErrorProvider, ErrorContext, useErrorReducer, useErrorContext };
