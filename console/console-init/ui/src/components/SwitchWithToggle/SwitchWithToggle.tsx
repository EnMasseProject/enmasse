/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { SwitchProps, Switch } from "@patternfly/react-core";

const SwitchWithToggle: React.FunctionComponent<SwitchProps> = ({
  id,
  label,
  onChange,
  labelOff,
  isChecked: checked = false,
  isDisabled
}) => {
  const [isChecked, setIsChecked] = useState<boolean>(checked);
  const handleOnChange = (checked: boolean, event: any) => {
    if (onChange) {
      onChange(checked, event);
    }
    setIsChecked(checked);
  };
  return (
    <Switch
      id={id}
      label={label}
      labelOff={labelOff}
      isChecked={isChecked}
      onChange={handleOnChange}
      isDisabled={isDisabled}
    />
  );
};
export { SwitchWithToggle };
