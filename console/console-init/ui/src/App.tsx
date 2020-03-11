/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import "@patternfly/react-core/dist/styles/base.css";
import { ErrorBoundary } from "./components/common/ErrorBoundary";
import "./App.css";
import { StoreProvider, useStore, rootReducer } from "context-state-reducer";
import AppLayout from "AppLayout";

const App: React.FC<{}> = () => {
  const [store] = useStore(rootReducer);

  return (
    <StoreProvider value={store}>
      <ErrorBoundary>
        <AppLayout />
      </ErrorBoundary>
    </StoreProvider>
  );
};

export default App;
