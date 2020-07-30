/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Nav, NavList, NavItem } from "@patternfly/react-core";
import { NavLink } from "react-router-dom";
import { StyleSheet, css } from "aphrodite";

export interface MessagingProjectNavigationProps {
  activeItem: string;
}

const styles = StyleSheet.create({
  nav_item_color: {
    color: "var(--pf-global--palette--black-1000)"
  }
});

export const MessagingProjectNavigation: React.FunctionComponent<MessagingProjectNavigationProps> = ({
  activeItem
}) => {
  const [active, setActive] = useState(activeItem);
  const onSelect1 = (result: any) => {
    setActive(result.itemId);
  };
  return (
    <Nav variant="tertiary" onSelect={onSelect1}>
      <NavList>
        <NavItem
          id="messaging-project-navigation-addresses-navitem"
          key="addresses"
          itemId="addresses"
          isActive={active === "addresses"}
        >
          <NavLink
            id="messaging-project-navigation-addresses-navlink"
            to={`addresses`}
            className={css(styles.nav_item_color)}
          >
            Addresses
          </NavLink>
        </NavItem>
        <NavItem
          id="messaging-project-navigation-connections-navitem"
          key="connections"
          itemId="connections"
          isActive={active === "connections"}
        >
          <NavLink
            id="messaging-project-navigation-connections-navlink"
            to={`connections`}
            className={css(styles.nav_item_color)}
          >
            Connections
          </NavLink>
        </NavItem>
        <NavItem
          id="messaging-project-navigation-endpoints-navitem"
          key="endpoints"
          itemId="endpoints"
          isActive={active === "endpoints"}
        >
          <NavLink
            id="messaging-project-navigation-endpoints-navlink"
            to={`endpoints`}
            className={css(styles.nav_item_color)}
          >
            Endpoints
          </NavLink>
        </NavItem>
      </NavList>
    </Nav>
  );
};
