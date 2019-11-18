import * as React from "react";
import { Label } from "@patternfly/react-core";

interface ITypePlanProps {
  plan: string;
  type: string;
}

export const TypePlan: React.FunctionComponent<ITypePlanProps> = address => {
  let iconColor = "";
  switch (address.type[0].toUpperCase()) {
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
  return (
    //TODO: Set background color
    <React.Fragment>
      <Label isCompact>{address.type[0].toUpperCase()}</Label> {address.plan}
    </React.Fragment>
  );
};
