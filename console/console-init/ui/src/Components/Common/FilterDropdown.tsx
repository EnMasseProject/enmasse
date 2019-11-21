import * as React from "react";
import { Dropdown, DropdownToggle, DropdownItem } from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";

export interface IDropdownOption {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface IDropdown {
  options: IDropdownOption[];
  value: string;
  onSelect: (item: any) => void;
}

export const FilterDropdown: React.FunctionComponent<IDropdown> = ({
  value,
  onSelect,
  options
}) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const onSelectFilter = (item: any) => {
    onSelect(item);
    setIsOpen(!isOpen);
  };
  return (
    <Dropdown
      position="left"
      onSelect={onSelectFilter}
      isOpen={isOpen}
      toggle={
        <DropdownToggle onToggle={setIsOpen}>
          <FilterIcon />
          &nbsp;{value}
        </DropdownToggle>
      }
      dropdownItems={options.map(option => (
        <DropdownItem
          key={option.value}
          value={option.value}
          itemID={option.value}>
          {option.label}
        </DropdownItem>
      ))}
    />
  );
};
