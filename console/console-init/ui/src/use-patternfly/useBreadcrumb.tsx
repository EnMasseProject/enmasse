/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { AppLayoutContext } from "./AppLayout";

export function useBreadcrumb(breadcrumb: React.ReactElement) {
  const context = React.useContext(AppLayoutContext);

  React.useEffect(() => {
    context.setBreadcrumb(breadcrumb);
    return () => {
      context.setBreadcrumb(null);
    };
  }, [context, breadcrumb]);
}
