/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Select,
  SelectVariant,
  SelectOption,
  SelectProps,
  SelectOptionObject
} from "@patternfly/react-core";
import { ISelectOption } from "utils";

export interface SelectWithToggleProps
  extends Omit<SelectProps, "onSelect" | "onToggle"> {
  selectOptions: ISelectOption[];
  onSelectItem?: (selection: string) => void;
  ariaLabel?: string;
  id?: string;
  optionId?: string;
}

export const SelectWithToggle: React.FC<SelectWithToggleProps> = ({
  selectOptions,
  onSelectItem,
  selections,
  ariaLabel,
  id,
  optionId,
  variant
}) => {
  const [isExpanded, setIsExpanded] = useState<boolean>();

  const onToggle = (isExpanded: boolean) => {
    setIsExpanded(isExpanded);
  };

  const onSelect = (_: any, selection: string | SelectOptionObject) => {
    const selectedValue =
      typeof selection === "string" ? selection : selection.toString();
    setIsExpanded(!isExpanded);
    onSelectItem && onSelectItem(selectedValue);
  };

  return (
    <Select
      id={id}
      variant={variant || SelectVariant.single}
      aria-label={ariaLabel}
      onToggle={onToggle}
      onSelect={onSelect}
      selections={selections}
      isExpanded={isExpanded}
    >
      {selectOptions.map((option, index) => (
        <SelectOption
          isDisabled={option.isDisabled}
          id={optionId + "-" + option.key}
          key={index}
          value={option.value}
        />
      ))}
    </Select>
  );
};
