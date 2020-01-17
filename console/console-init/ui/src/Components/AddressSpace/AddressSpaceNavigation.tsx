/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { Nav, NavList, NavVariants, NavItem } from "@patternfly/react-core";
import { NavLink } from "react-router-dom";

export interface AddressSpaceNavigationProps {
  activeItem: string;
}
export const AddressSpaceNavigation: React.FunctionComponent<AddressSpaceNavigationProps> = ({
  activeItem
}) => {
  const [active, setActive] = React.useState(activeItem);
  const onSelect1 = (result: any) => {
    setActive(result.itemId);
  };
  return (
    <Nav onSelect={onSelect1}>
      <NavList variant={NavVariants.tertiary}>
        <NavItem
          key="addresses"
          itemId="addresses"
          isActive={active === "addresses"}
        >
          <NavLink id='ad-space-nav-addresses' to={`addresses`} style={{ color: "black" }}>
            Addresses
          </NavLink>
        </NavItem>
        <NavItem
          key="connections"
          itemId="connections"
          isActive={active === "connections"}
        >
          <NavLink id='ad-space-nav-connections' to={`connections`} style={{ color: "black" }}>
            Connections
          </NavLink>
        </NavItem>
      </NavList>
    </Nav>
  );
};
