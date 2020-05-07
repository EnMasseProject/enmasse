/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import { Nav, NavList, NavItem, NavVariants } from "@patternfly/react-core";
import { NavLink } from "react-router-dom";
import { StyleSheet, css } from "@patternfly/react-styles";

interface INavigationProps {
  activeItem: string;
}

const styles = StyleSheet.create({
  navlink: {
    color: "black",
    paddingLeft: 10,
    paddingRight: 10
  }
});

const DeviceDetailNavigation: React.FunctionComponent<INavigationProps> = ({
  activeItem
}) => {
  const [active, setActive] = useState();
  useEffect(() => {
    setActive(activeItem);
  }, [activeItem]);
  const onSelect1 = (result: any) => {
    setActive(result.itemId);
  };
  return (
    <Nav onSelect={onSelect1}>
      <NavList variant={NavVariants.tertiary}>
        <NavItem
          key="deviceInfo"
          itemId="deviceInfo"
          isActive={active === "deviceInfo"}
        >
          <NavLink
            id="nav-device-info"
            to={`detail`}
            className={css(styles.navlink)}
          >
            Device Info
          </NavLink>
        </NavItem>
        <NavItem
          key="liveDataStream"
          itemId="liveDataStream"
          isActive={active === "liveDataStream"}
        >
          <NavLink
            id="nav-device-live-data-stream"
            to={`live-stream`}
            className={css(styles.navlink)}
          >
            Live Data Stream
          </NavLink>
        </NavItem>
        <NavItem
          key="connectionInfo"
          itemId="connectionInfo"
          isActive={active === "connectionInfo"}
        >
          <NavLink
            id="av-device-connection-info"
            to={`connection-info`}
            className={css(styles.navlink)}
          >
            Connection Info
          </NavLink>
        </NavItem>
      </NavList>
    </Nav>
  );
};

export { DeviceDetailNavigation };
