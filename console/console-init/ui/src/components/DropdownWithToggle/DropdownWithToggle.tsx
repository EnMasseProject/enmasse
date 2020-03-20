/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import classNames from "classnames";
import {
  Dropdown,
  DropdownToggle,
  DropdownItem,
  DropdownProps,
  DropdownPosition,
  DropdownToggleProps
} from "@patternfly/react-core";
import { css, StyleSheet } from "@patternfly/react-styles";

export const dropdown_styles = StyleSheet.create({
  format_description: { whiteSpace: "normal", textAlign: "justify" },
  font_item: { fontWeight: "bold" }
});

export interface IDropdownWithToggleProps
  extends Omit<DropdownProps, "toggle" | "onSelect"> {
  id: string;
  toggleId?: string;
  toggleIcon?: React.ReactElement<any>;
  component?: React.ReactNode;
  toggleClass?: string;
  value: string;
  isItemDisplayBold?: boolean;
  onSelectItem?: (value: string) => void;
}

export const DropdownWithToggle: React.FC<IDropdownWithToggleProps &
  DropdownToggleProps> = ({
  id,
  toggleId,
  position,
  onSelectItem,
  toggleIcon,
  dropdownItems,
  component,
  className,
  toggleClass,
  value,
  isDisabled,
  isItemDisplayBold
}) => {
  const [isOpen, setIsOpen] = useState<boolean>();

  const dropdowItemCss = classNames({
    [dropdown_styles.font_item]: isItemDisplayBold
  });

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const onSelect = (e: any) => {
    const value = e.target.value || e.target.textContent;
    setIsOpen(!isOpen);
    if (onSelectItem) {
      onSelectItem(value);
    }
  };

  const getDropdownItems = () => {
    let items: any[] = [];
    if (dropdownItems && dropdownItems.length > 0) {
      items = dropdownItems.map(option => (
        <DropdownItem
          id={`filter-dropdown-item${option.key}`}
          key={option.key}
          value={option.value}
          itemID={option.key}
          component={component || "button"}
        >
          <span className={dropdowItemCss}>{option.value}</span>
          {option.description && (
            <div className={css(dropdown_styles.format_description)}>
              {option.description}
            </div>
          )}
        </DropdownItem>
      ));
    }
    return items;
  };

  return (
    <Dropdown
      id={id}
      className={className}
      position={position || DropdownPosition.left}
      onSelect={onSelect}
      isOpen={isOpen}
      toggle={
        <DropdownToggle
          id={toggleId || id}
          onToggle={onToggle}
          className={toggleClass}
          isDisabled={isDisabled}
        >
          {toggleIcon}
          {value}
        </DropdownToggle>
      }
      dropdownItems={getDropdownItems()}
    />
  );
};
