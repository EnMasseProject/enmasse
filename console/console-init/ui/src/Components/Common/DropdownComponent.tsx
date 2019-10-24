import * as React from 'react';
import { Dropdown, DropdownToggle, DropdownItem } from '@patternfly/react-core';
import { FilterIcon } from '@patternfly/react-icons';

export interface IDropdownOption {
  value: string;
  label: string;
}
export interface IDropdown {
  options: IDropdownOption[];
  isOpen: boolean;
  setIsOpen: () => void;
  value: string;
  onSelect: (item: any) => void;
}
const DropdownComponent: React.FunctionComponent<IDropdown> = ({ isOpen, setIsOpen, value, onSelect, options }) => {
  return (
    <Dropdown
      position="left"
      onSelect={onSelect}
      isOpen={isOpen}
      toggle={
        <DropdownToggle onToggle={setIsOpen}>
          <FilterIcon />
          &nbsp;{value}
        </DropdownToggle>
      }
      dropdownItems={options.map(option => (
        <DropdownItem key={option.value} value={option.value} itemID={option.value}>
          {option.label}
        </DropdownItem>
      ))}
    />
  );
};

export default DropdownComponent;
