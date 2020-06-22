/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { RouteProps, Route } from "react-router-dom";
import { Loading } from "./Loading";

export interface IDynamicImportProps extends RouteProps {
  getComponent: () => Promise<{ default: React.ComponentType }>;
}

export function LazyRoute({ getComponent, ...props }: IDynamicImportProps) {
  const LazyComponent = React.useMemo(() => React.lazy(getComponent), [
    getComponent
  ]);
  return (
    <Route {...props}>
      <React.Suspense fallback={<Loading />}>
        <LazyComponent />
      </React.Suspense>
    </Route>
  );
}
