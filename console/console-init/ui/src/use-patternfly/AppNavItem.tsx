/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as H from "history";
import React from "react";
import { NavLink } from "react-router-dom";
import { NavItem, NavItemSeparator } from "@patternfly/react-core";

export interface IAppNavItemProps<S = {}> {
  title?: string;
  to?:
    | H.LocationDescriptor<S>
    | ((location: H.Location<S>) => H.LocationDescriptor<S>);
  exact?: boolean;
}

export function AppNavItem<S>({ title, to, exact }: IAppNavItemProps<S>) {
  if (!title || !to) {
    return <NavItemSeparator data-testid={"navitem-separator"} />;
  }
  return (
    <NavItem>
      <NavLink to={to} exact={exact} activeClassName="pf-m-current">
        {title}
      </NavLink>
    </NavItem>
  );
}
