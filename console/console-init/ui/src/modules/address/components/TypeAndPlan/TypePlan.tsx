/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Tooltip, TooltipPosition, Badge } from "@patternfly/react-core";
import { getTypeColor } from "utils";

interface ITypePlanProps {
  plan: string;
  type: string;
}

export const TypePlan: React.FunctionComponent<ITypePlanProps> = address => {
  const iconColor = getTypeColor(address.type[0]);
  const labelItem = (
    <Badge
      id="type-plan-badge"
      style={{ backgroundColor: iconColor, fontSize: 12, padding: 5 }}
    >
      {address.type[0].toUpperCase() + " "}
    </Badge>
  );
  return (
    <div>
      <Tooltip
        id="type-tooltip"
        position={TooltipPosition.top}
        content={<div>{address.type}</div>}
      >
        {labelItem}
      </Tooltip>
      {" " + address.plan}
    </div>
  );
};
