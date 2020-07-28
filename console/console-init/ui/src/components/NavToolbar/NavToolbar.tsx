/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownPosition
} from "@patternfly/react-core";
import { QuestionCircleIcon } from "@patternfly/react-icons";
import { About, User } from "components";
import { StyleSheet, css } from "aphrodite";

const styles = StyleSheet.create({
  navtb_dropdownitem_help: {
    paddingRight: 100,
    paddingLeft: 20,
    paddingTop: 20,
    paddingBottom: 10
  },
  navtb_dropdownitem_about: {
    paddingRight: 100,
    paddingLeft: 20,
    paddingTop: 10,
    paddingBottom: 20
  }
});

const NavToolBar: React.FC = () => {
  const [isOpen, onToggle] = useState(false);
  const [isAboutModalOpen, setIsAboutModalOpen] = useState<boolean>(false);
  const selectAbout = () => {
    setIsAboutModalOpen(true);
  };
  const [isUserDropdownOpen, setIsUserDropdownOpen] = useState<boolean>(false);

  const onUserDropdownSelect = () => {
    setIsUserDropdownOpen(!isUserDropdownOpen);
  };
  const dropdownItems = [
    <a href={process.env.REACT_APP_DOCS} key="help">
      <DropdownItem
        id="nav-toolbar-help-dropdownitem"
        className={css(styles.navtb_dropdownitem_help)}
      >
        Help
      </DropdownItem>
    </a>,
    <DropdownItem
      id="nav-toolbar-about-dropdownitem"
      key="About"
      className={css(styles.navtb_dropdownitem_about)}
      onClick={selectAbout}
    >
      About
    </DropdownItem>
  ];
  const userDropdownItems = [
    <DropdownItem
      id="nav-toolbar-logout-dropdownitem"
      key={"logout"}
      href="oauth/sign_in"
    >
      Logout
    </DropdownItem>
  ];
  return (
    <React.Fragment>
      <Dropdown
        id="nav-toolbar-help-about-dropdown"
        key="help-about-dropdown"
        position={DropdownPosition.right}
        toggle={
          <DropdownToggle
            id="nav-toolbar-applications-dropdown-toggle"
            toggleIndicator={null}
            onToggle={onToggle}
            aria-label="Applications"
          >
            <QuestionCircleIcon />
          </DropdownToggle>
        }
        isOpen={isOpen}
        isPlain
        dropdownItems={dropdownItems}
      />
      <Dropdown
        id="nav-toolbar-user-dropdown"
        key="user-dropdown"
        isPlain
        position="right"
        onSelect={onUserDropdownSelect}
        isOpen={isUserDropdownOpen}
        toggle={
          <DropdownToggle onToggle={setIsUserDropdownOpen}>
            <User />
          </DropdownToggle>
        }
        dropdownItems={userDropdownItems}
      />

      <About
        isAboutModalOpen={isAboutModalOpen}
        handleAboutModalToggle={() => setIsAboutModalOpen(!isAboutModalOpen)}
      />
    </React.Fragment>
  );
};

export { NavToolBar };
