/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Dropdown,
  KebabToggle,
  DropdownProps,
  DropdownPosition,
  KebabToggleProps
} from "@patternfly/react-core";

export interface IDropdownWithKebabToggleProps
  extends Omit<DropdownProps, "toggle"> {
  toggleId?: string;
}

export const DropdownWithKebabToggle: React.FC<IDropdownWithKebabToggleProps &
  KebabToggleProps> = ({
  id,
  className,
  position,
  onSelect,
  isPlain,
  dropdownItems,
  toggleId
}) => {
  const [isOpen, setIsOpen] = useState<boolean>();

  const onToggle = (isOpen: boolean) => {
    setIsOpen(isOpen);
  };

  return (
    <Dropdown
      id={id}
      isPlain={isPlain}
      className={className}
      position={position || DropdownPosition.right}
      onSelect={onSelect}
      isOpen={isOpen}
      toggle={<KebabToggle id={toggleId} onToggle={onToggle} />}
      dropdownItems={dropdownItems}
    />
  );
};
