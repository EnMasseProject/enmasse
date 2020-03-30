/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  IconSize,
  ToggleOnIcon as OnIcon,
  ToggleOffIcon as OffIcon
} from "@patternfly/react-icons";

export type OneOf<T, K extends keyof T> = T[K];

export type IconType = React.FC<any>;

export interface IToggleIconProps {
  isEnabled: boolean;
  onToggle: (isEnabled: boolean, event?: any) => void;
  toggleOnIconColor?: string;
  toggleOffIconColor?: string;
  iconSize?: OneOf<typeof IconSize, keyof typeof IconSize>;
  enabledTitle?: string;
  disabledTitle?: string;
  name?: string;
  ToggleOnIcon?: IconType;
  ToggleOffIcon?: IconType;
}

export const ToggleIcon: React.FC<IToggleIconProps> = ({
  isEnabled,
  onToggle,
  toggleOnIconColor,
  toggleOffIconColor,
  iconSize,
  enabledTitle,
  disabledTitle,
  name,
  ToggleOnIcon = OnIcon,
  ToggleOffIcon = OffIcon
}) => {
  const onToggleIcon = (isEnabled: boolean, event: any) => {
    if (name && !event.target.name) {
      event.target.name = name;
    }
    onToggle(isEnabled, event);
  };

  return isEnabled ? (
    <span>
      <ToggleOnIcon
        onClick={(event: any) => onToggleIcon(false, event)}
        color={toggleOnIconColor || "var(--pf-global--active-color--100)"}
        size={iconSize || IconSize.lg}
      />
      {enabledTitle && <span>&nbsp; {enabledTitle}</span>}
    </span>
  ) : (
    <span>
      <ToggleOffIcon
        onClick={(event: any) => onToggleIcon(true, event)}
        size={iconSize || IconSize.lg}
        color={toggleOffIconColor}
      />
      {disabledTitle && <span>&nbsp; {disabledTitle}</span>}
    </span>
  );
};
