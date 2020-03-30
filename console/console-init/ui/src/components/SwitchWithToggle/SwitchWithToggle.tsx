/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { SwitchProps, Switch } from "@patternfly/react-core";

const SwitchWithToggle: React.FunctionComponent<SwitchProps> = ({
  id,
  label,
  labelOff
}) => {
  const [isChecked, setIsChecked] = useState<boolean>(false);
  const onToggle = () => {
    setIsChecked(!isChecked);
  };
  return (
    <Switch
      id={id}
      label={label}
      labelOff={labelOff}
      isChecked={isChecked}
      onChange={onToggle}
    />
  );
};
export { SwitchWithToggle };
