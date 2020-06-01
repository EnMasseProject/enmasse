/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  IconSize,
  CheckCircleIcon as OnIcon,
  ErrorCircleOIcon as OffIcon
} from "@patternfly/react-icons";

export type OneOf<T, K extends keyof T> = T[K];
export type IconType = React.FC<any>;

export interface IStatusLabelWithIconProps {
  id: string;
  isEnabled: boolean;
  enabledIconColor?: string;
  disabledIconColor?: string;
  iconSize?: OneOf<typeof IconSize, keyof typeof IconSize>;
  enabledTitle?: string;
  disabledTitle?: string;
  EnabledIcon?: IconType;
  DisabledIcon?: IconType;
}

export const StatusLabelWithIcon: React.FC<IStatusLabelWithIconProps> = ({
  id,
  isEnabled,
  EnabledIcon = OnIcon,
  DisabledIcon = OffIcon,
  enabledTitle = "Enabled",
  disabledTitle = "Disabled",
  iconSize = IconSize.sm,
  enabledIconColor = "var(--pf-global--palette--green-400)",
  disabledIconColor = "var(--pf-global--palette--red-100)"
}) => {
  return (
    <div id={id}>
      {isEnabled ? (
        <>
          <EnabledIcon color={enabledIconColor} size={iconSize} />
          &nbsp;{enabledTitle}
        </>
      ) : (
        <>
          <DisabledIcon color={disabledIconColor} size={iconSize} />
          &nbsp;{disabledTitle}
        </>
      )}
    </div>
  );
};
