/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Nav, NavList, NavItem } from "@patternfly/react-core";
import { NavLink } from "react-router-dom";
import { StyleSheet, css } from "aphrodite";

export interface ProjectNavigationProps {
  activeItem: string;
}

const styles = StyleSheet.create({
  navlink: {
    color: "black",
    paddingLeft: 10,
    paddingRight: 10,
    marginRight: 20
  }
});

export const ProjectNavigation: React.FunctionComponent<ProjectNavigationProps> = ({
  activeItem
}) => {
  const [active, setActive] = useState(activeItem.toLowerCase());
  const onSelect1 = (result: any) => {
    setActive(result.itemId);
  };
  return (
    <Nav variant="tertiary" onSelect={onSelect1}>
      <NavList>
        <NavItem
          key="detail"
          itemId="detail"
          isActive={active === "detail"}
          id="project-navigation-detail-navitem"
        >
          <NavLink
            id="project-navigation-detail-navlink"
            aria-label="navigate to detail"
            to={`detail`}
            className={css(styles.navlink)}
          >
            Detail
          </NavLink>
        </NavItem>
        <NavItem
          key="devices"
          itemId="devices"
          isActive={active === "devices"}
          id="project-navigation-devices-navitem"
        >
          <NavLink
            id="project-navigation-devices-navlink"
            aria-label="navigate to devices"
            to={`devices`}
            className={css(styles.navlink)}
          >
            Devices
          </NavLink>
        </NavItem>
        <NavItem
          id="project-navigation-certificates-navitem"
          key="certificates"
          itemId="certificates"
          isActive={active === "certificates"}
        >
          <NavLink
            id="project-navigation-certificates-navlink"
            aria-label="navigate to certificates"
            to={`certificates`}
            className={css(styles.navlink)}
          >
            Device certificates
          </NavLink>
        </NavItem>
      </NavList>
    </Nav>
  );
};
