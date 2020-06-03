/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Nav, NavList, NavItem, NavVariants } from "@patternfly/react-core";
import { NavLink } from "react-router-dom";
import { StyleSheet, css } from "@patternfly/react-styles";

export interface INavigationProps {
  activeItem: string;
}

const styles = StyleSheet.create({
  navlink: {
    color: "black",
    paddingLeft: 10,
    paddingRight: 10
  }
});
export interface INavSelectedItem {
  groupId: number | string;
  itemId: number | string;
  to: string;
  event: React.FormEvent<HTMLInputElement>;
}

const DeviceDetailNavigation: React.FunctionComponent<INavigationProps> = ({
  activeItem
}) => {
  const [active, setActive] = useState(activeItem);

  const onSelect = (result: INavSelectedItem) => {
    setActive(result.itemId.toString());
  };

  return (
    <Nav onSelect={onSelect}>
      <NavList variant={NavVariants.tertiary}>
        <NavItem
          key="device-info"
          itemId="deviceinfo"
          isActive={active === "deviceinfo"}
        >
          <NavLink
            id="nav-device-info"
            to={`deviceinfo`}
            className={css(styles.navlink)}
          >
            Device Info
          </NavLink>
        </NavItem>
        {/**
         * TODO: Live stream will implement later
         */}
        {/* <NavItem
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
        </NavItem> */}
        <NavItem
          key="configuration-info"
          itemId="configurationinfo"
          isActive={active === "configurationinfo"}
        >
          <NavLink
            id="nav-device-configuration-info"
            to={`configurationinfo`}
            className={css(styles.navlink)}
          >
            Configuration Info
          </NavLink>
        </NavItem>
      </NavList>
    </Nav>
  );
};

export { DeviceDetailNavigation };
