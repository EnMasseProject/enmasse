/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { Tooltip, TooltipPosition, Badge } from "@patternfly/react-core";

interface ITypePlanProps {
  plan: string;
  type: string;
}

export const getTypeColor = (type: string) => {
  let iconColor = "";
  switch (type.toUpperCase()) {
    case "Q": {
      iconColor = "#8A8D90";
      break;
    }
    case "T": {
      iconColor = "#8481DD";
      break;
    }
    case "S": {
      iconColor = "#EC7A08";
      break;
    }
    case "M": {
      iconColor = "#009596";
      break;
    }
    case "A": {
      iconColor = "#F4C145";
      break;
    }
  }
  return iconColor;
};

interface ITypeString {
  type: string;
}

export const TypeBadge: React.FunctionComponent<ITypeString> = ({ type }) => {
  const iconColor = getTypeColor(type[0]);
  return (
    <Badge
      style={{
        backgroundColor: iconColor,
        fontSize: 25,
        paddingLeft: 15,
        paddingRight: 15,
        paddingTop: 5,
        paddingBottom: 5
      }}
    >
      {type[0].toUpperCase()}
    </Badge>
  );
};

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
    <>
      <Tooltip
        id="type-tooltip"
        position={TooltipPosition.top}
        content={<div>{address.type}</div>}
      >
        {labelItem}
      </Tooltip>
      {" " + address.plan}
    </>
  );
};
