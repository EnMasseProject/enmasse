/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Badge } from "@patternfly/react-core";
import { getTypeColor } from "utils";

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
