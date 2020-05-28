/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Nav, NavList, NavVariants, NavItem } from "@patternfly/react-core";
import { NavLink } from "react-router-dom";
import { StyleSheet, css } from "@patternfly/react-styles";

export interface ProjectNavigationProps {
  activeItem: string;
}

const styles = StyleSheet.create({
  navlink: {
    color: "black",
    paddingLeft: 10,
    paddingRight: 10
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
    <Nav onSelect={onSelect1}>
      <NavList variant={NavVariants.tertiary}>
        <NavItem key="detail" itemId="detail" isActive={active === "detail"}>
          <NavLink
            id="ad-space-nav-detail"
            to={`detail`}
            className={css(styles.navlink)}
          >
            Detail
          </NavLink>
        </NavItem>
        <NavItem key="devices" itemId="devices" isActive={active === "devices"}>
          <NavLink
            id="ad-space-nav-devices"
            to={`devices`}
            className={css(styles.navlink)}
          >
            Devices
          </NavLink>
        </NavItem>
        <NavItem
          key="certificates"
          itemId="certificates"
          isActive={active === "certificates"}
        >
          <NavLink
            id="ad-space-nav-certificates"
            to={`certificates`}
            className={css(styles.navlink)}
          >
            Certificates
          </NavLink>
        </NavItem>
      </NavList>
    </Nav>
  );
};
