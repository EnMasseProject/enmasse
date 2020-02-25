/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import "@patternfly/react-core/dist/styles/base.css";
import { ErrorBoundary } from "./components/common/ErrorBoundary";
import "./App.css";
import { ErrorProvider, useErrorReducer } from "context-state-reducer";
import AppLayout from "AppLayout";

const App: React.FC<{}> = () => {
  const [contextValue] = useErrorReducer();

  return (
    <ErrorProvider value={contextValue}>
      {/* <ErrorBoundary> */}
      <AppLayout />
      {/* </ErrorBoundary> */}
    </ErrorProvider>
  );
};

export default App;
