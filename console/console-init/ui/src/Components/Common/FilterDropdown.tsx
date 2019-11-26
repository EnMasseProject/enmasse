import * as React from "react";
import { Dropdown, DropdownToggle, DropdownItem } from "@patternfly/react-core";
import { FilterIcon } from "@patternfly/react-icons";

export interface IDropdownOption {
  value: string;
  label: string;
  disabled?: boolean;
  description?: string;
}

export interface IDropdown {
  options: IDropdownOption[];
  value: string;
  setValue: (value: any) => void;
  // onSelect: (item: any) => void;
}

export const FilterDropdown: React.FunctionComponent<IDropdown> = ({
  value,
  setValue,
  options
}) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const onSelectFilter = (event: any) => {
    setValue(event.target.value);
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
          itemID={option.value}
          component={"button"}
        >
          {option.label}
        </DropdownItem>
      ))}
    />
  );
};
