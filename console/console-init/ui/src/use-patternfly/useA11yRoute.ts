/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { LastLocationType, useLastLocation } from "react-router-last-location";

export function accessibleRouteChangeHandler(id: string, timeout = 50) {
  return window.setTimeout(() => {
    const mainContainer = document.getElementById(id);
    if (mainContainer) {
      mainContainer.focus();
    }
  }, timeout);
}

/**
 * a custom hook for sending focus to the primary content container
 * after a view has loaded so that subsequent press of tab key
 * sends focus directly to relevant content
 */
export const useA11yRouteChange = (id = "main-container") => {
  const lastNavigation = useLastLocation();
  const previousNavigation = React.useRef<LastLocationType | null>();
  React.useEffect(() => {
    let routeFocusTimer: number;
    let isStale = true;
    if (lastNavigation !== null) {
      previousNavigation.current = lastNavigation;
      isStale = false;
      routeFocusTimer = accessibleRouteChangeHandler(id, 50);
    }
    return () => {
      if (routeFocusTimer && isStale) {
        clearTimeout(routeFocusTimer);
      }
    };
  }, [id, lastNavigation, previousNavigation]);
};
