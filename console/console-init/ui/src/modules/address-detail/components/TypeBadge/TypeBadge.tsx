import * as React from "react";
import { getTypeColor } from "utils";
import { Badge } from "@patternfly/react-core";

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
