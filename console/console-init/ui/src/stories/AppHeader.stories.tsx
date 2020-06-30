/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { boolean } from "@storybook/addon-knobs";
import { action } from "@storybook/addon-actions";
import { AppLayout } from "use-patternfly";
import {
  Brand,
  Text,
  TextVariants,
  Dropdown,
  DropdownToggle,
  DropdownPosition,
  DropdownItem
} from "@patternfly/react-core";
import { CogIcon } from "@patternfly/react-icons";
import brandImg from "assets/images/logo.svg";

export default {
  title: "AppHeader"
};

const Avatar = () => (
  <React.Fragment>
    <Text component={TextVariants.p}>Ramakrishna Pattnaik</Text>
  </React.Fragment>
);
const dropdownItems = [
  <DropdownItem key="help">Help</DropdownItem>,
  <DropdownItem key="About">About</DropdownItem>
];
const NavToolBar = () => (
  <Dropdown
    position={DropdownPosition.right}
    toggle={
      <DropdownToggle toggleIndicator={null} aria-label="Applications">
        <CogIcon />
      </DropdownToggle>
    }
    isOpen={boolean("keep toolbar open", true)}
    isPlain
    dropdownItems={dropdownItems}
  />
);
const logo = <Brand src={brandImg} alt="Console Logo" />;

const HeaderTools = () => (
  <div className="pf-c-page__header-tools">
    <NavToolBar />
    <Avatar />
  </div>
);

export const pageHeader = () => (
  <AppLayout
    logoProps={{
      onClick: action("Logo clicked")
    }}
    logo={logo}
    headerTools={<HeaderTools />}
  ></AppLayout>
);
