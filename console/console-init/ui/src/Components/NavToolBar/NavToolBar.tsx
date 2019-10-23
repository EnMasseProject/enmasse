import React, { useState } from 'react';
import { Dropdown, DropdownToggle, DropdownItem, DropdownPosition } from '@patternfly/react-core';
import { CogIcon } from '@patternfly/react-icons';
import './NavToolBar.css';

const NavToolBar:React.FC = () => {

  const [isOpen, onToggle] = useState(false);

  const dropdownItems = [
    <DropdownItem key="help">Help</DropdownItem>,
    <DropdownItem key="About">About</DropdownItem>
  ];
  return (
    <React.Fragment>
      <Dropdown
        position={DropdownPosition.right}
        toggle={
          <DropdownToggle iconComponent={null} onToggle={onToggle} aria-label="Applications">
            <CogIcon />
          </DropdownToggle>
        }
        isOpen={isOpen}
        isPlain
        dropdownItems={dropdownItems}
      />
    </React.Fragment>
  );
}

export default NavToolBar;