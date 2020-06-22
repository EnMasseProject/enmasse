/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { NavGroup } from "@patternfly/react-core";
import { AppNavItem, IAppNavItemProps } from "./AppNavItem";

export interface IAppNavGroupProps {
  title: string;
  items: Array<IAppNavItemProps | undefined>;
}

export const AppNavGroup: React.FunctionComponent<IAppNavGroupProps> = ({
  title,
  items
}) => {
  return (
    <NavGroup title={title}>
      {items.map((subNavItem, jdx) => (
        <AppNavItem {...subNavItem} key={jdx} />
      ))}
    </NavGroup>
  );
};
