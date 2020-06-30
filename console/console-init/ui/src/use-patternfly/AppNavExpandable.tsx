/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useRouteMatch } from "react-router-dom";
import { NavExpandable } from "@patternfly/react-core";
import { AppNavItem, IAppNavItemProps } from "./AppNavItem";

export interface IAppNavExpandableProps {
  title: string;
  to: string;
  items: Array<IAppNavItemProps | undefined>;
}

export const AppNavExpandable: React.FunctionComponent<IAppNavExpandableProps> = ({
  title,
  to,
  items
}) => {
  const match = useRouteMatch({
    path: to
  });
  const isActive = !!match;
  return (
    <NavExpandable title={title} isActive={isActive} isExpanded={isActive}>
      {items.map((subNavItem, jdx) => (
        <AppNavItem {...subNavItem} key={jdx} />
      ))}
    </NavExpandable>
  );
};
