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
import { About } from "components/common/About";
import { UserDetail } from "./User";

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
    <a href={process.env.REACT_APP_DOCS}>
      <DropdownItem
        id="navtb-item-help"
        key="help"
        style={{
          paddingRight: 100,
          paddingLeft: 20,
          paddingTop: 20,
          paddingBottom: 10
        }}
      >
        Help
      </DropdownItem>
    </a>,
    <DropdownItem
      id="navtb-item-about"
      key="About"
      style={{
        paddingRight: 100,
        paddingLeft: 20,
        paddingTop: 10,
        paddingBottom: 20
      }}
      onClick={selectAbout}
    >
      About
    </DropdownItem>
  ];
  const userDropdownItems = [
    <DropdownItem id="dd-menuitem-logout" key={"logout"} href="oauth/sign_in">
      Logout
    </DropdownItem>
  ];
  return (
    <React.Fragment>
      <Dropdown
        id="navtb-item-help-about"
        position={DropdownPosition.right}
        toggle={
          <DropdownToggle
            iconComponent={null}
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
        id="dd-user"
        isPlain
        position="right"
        onSelect={onUserDropdownSelect}
        isOpen={isUserDropdownOpen}
        toggle={
          <DropdownToggle onToggle={setIsUserDropdownOpen}>
            <UserDetail />
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

export default NavToolBar;
