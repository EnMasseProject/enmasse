/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Nav, NavList, NavItem } from "@patternfly/react-core";
import { NavLink } from "react-router-dom";
import {} from "@patternfly/react-styles";

export interface INavigationProps {
  activeItem: string;
}

// const styles = StyleSheet.create({
//   navlink: {
//     color: "black",
//     paddingLeft: 10,
//     paddingRight: 10
//   }
// });
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
    <Nav variant="tertiary" onSelect={onSelect}>
      <NavList>
        <NavItem
          key="device-info"
          itemId="device-info"
          isActive={active === "device-info"}
        >
          <NavLink
            id="nav-device-info"
            to={`device-info`}
            // className={css(styles.navlink)}
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
          itemId="configuration-info"
          isActive={active === "configuration-info"}
        >
          <NavLink
            id="nav-device-configuration-info"
            to={`configuration-info`}
            // className={css(styles.navlink)}
          >
            Configuration Info
          </NavLink>
        </NavItem>
      </NavList>
    </Nav>
  );
};

export { DeviceDetailNavigation };
