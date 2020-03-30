/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
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
  dropdown_alignment: { minHeight: "37px" }
});

export interface IDropdownOption {
  value: string;
  label: string;
  disabled?: boolean;
  description?: string;
  key?: string;
}

export interface IDropdownWithToggleProps
  extends Omit<DropdownProps, "toggle" | "onSelect"> {
  id: string;
  toggleId?: string;
  toggleIcon?: React.ReactElement<any>;
  component?: React.ReactNode;
  toggleClass?: string;
  value: string;
  onSelectItem?: (value: string, event?: any) => void;
  dropdownItemIdPrefix?: string;
  dropdownItemClass?: string;
  shouldDisplayLabelAndValue?: boolean;
  isLabelAndValueNotSame?: boolean;
  name?: string;
}

export const DropdownWithToggle: React.FC<IDropdownWithToggleProps &
  DropdownToggleProps> = ({
  id,
  name,
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
  dropdownItemIdPrefix,
  dropdownItemClass,
  shouldDisplayLabelAndValue,
  isLabelAndValueNotSame
}) => {
  const [isOpen, setIsOpen] = useState<boolean>();
  const dropdowItemCss = classNames(dropdownItemClass);
  const dropdownClass = classNames(
    css(dropdown_styles.dropdown_alignment),
    className
  );

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  const onFocus = () => {
    const element = document.getElementById(toggleId || id);
    element && element.focus();
  };

  /*
   *  If there is only one option use it as default
   */
  useEffect(() => {
    if (!value || value.trim() === "") {
      if (dropdownItems && dropdownItems.length === 1) {
        const item = dropdownItems[0];
        if (item && item.value && onSelectItem) {
          onSelectItem(item.value);
        }
      }
    }
  }, [dropdownItems]);

  const onSelect = (e: any) => {
    const value =
      e.target.value ||
      e.currentTarget.firstChild.value ||
      e.target.textContent;
    setIsOpen(!isOpen);
    onFocus();
    if (onSelectItem) {
      if (name && !e.target.name) {
        e.target.name = name;
      }
      onSelectItem(value, e);
    }
  };

  const getItems = (option: IDropdownOption) => {
    if (shouldDisplayLabelAndValue) {
      return (
        <>
          <span className={dropdowItemCss}>{option.label || option.value}</span>
          <div>{option.value}</div>
          {option.description && (
            <div className={css(dropdown_styles.format_description)}>
              {option.description}
            </div>
          )}
        </>
      );
    }

    return (
      <>
        <span className={dropdowItemCss}>{option.label || option.value}</span>
        {option.description && (
          <div className={css(dropdown_styles.format_description)}>
            {option.description}
          </div>
        )}
      </>
    );
  };

  const getDropdownItems = () => {
    let items: React.ReactElement<IDropdownOption>[] = [];
    if (dropdownItems && dropdownItems.length > 0) {
      items = dropdownItems.map((option: IDropdownOption) => (
        <DropdownItem
          id={`${dropdownItemIdPrefix || id}${option.key}`}
          key={option.key}
          value={option.value}
          itemID={option.key}
          component={component || "button"}
          label={option.label}
        >
          {getItems(option)}
        </DropdownItem>
      ));
    }
    return items;
  };

  const getSelectedValue = () => {
    if (isLabelAndValueNotSame) {
      const filteredOption = dropdownItems?.filter(
        item => item.value === value
      )[0];
      return filteredOption?.label;
    }
    return value;
  };

  return (
    <Dropdown
      id={id}
      name={name}
      className={dropdownClass}
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
          {getSelectedValue()}
        </DropdownToggle>
      }
      dropdownItems={getDropdownItems()}
    />
  );
};
