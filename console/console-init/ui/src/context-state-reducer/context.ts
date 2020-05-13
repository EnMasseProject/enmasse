/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { createContext, useReducer, useContext, useMemo } from "react";
import { initialState, IInitialState } from "./initialState";

const StoreContext = createContext<any>(initialState);
const StoreProvider = StoreContext.Provider;

const useStore = (rootReducer: any, initialState?: IInitialState) => {
  const initialStates =
    initialState || rootReducer(undefined, { type: undefined });
  const [state, dispatch] = useReducer(rootReducer, initialStates);

  const contextValue = useMemo(() => {
    return { state, dispatch };
  }, [state, dispatch]);
  return [contextValue];
};

const useStoreContext = () => {
  return useContext(StoreContext);
};

export { StoreProvider, StoreContext, useStore, useStoreContext };
