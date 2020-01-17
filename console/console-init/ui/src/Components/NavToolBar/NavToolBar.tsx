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

const NavToolBar: React.FC = () => {
  const [isOpen, onToggle] = useState(false);

  const dropdownItems = [
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
    </DropdownItem>,
    <DropdownItem
      id="navtb-item-about"
      key="About"
      style={{
        paddingRight: 100,
        paddingLeft: 20,
        paddingTop: 10,
        paddingBottom: 20
      }}
    >
      About
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
    </React.Fragment>
  );
};

export default NavToolBar;
