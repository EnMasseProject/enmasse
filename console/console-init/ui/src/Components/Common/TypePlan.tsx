import * as React from "react";
import {
  Label,
  Tooltip,
  TooltipPosition
  // Tooltip,
  // TooltipPosition
} from "@patternfly/react-core";
import { OutlinedQuestionCircleIcon } from "@patternfly/react-icons";

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
  const labelItem = (
    <Label isCompact style={{ backgroundColor: iconColor }}>
      {address.type[0].toUpperCase() + " "}
    </Label>
  );
  return (
    <React.Fragment>
      <Tooltip
        position={TooltipPosition.top}
        content={<div>{address.type}</div>}>
        {labelItem}
      </Tooltip>
      {" " + address.plan}
    </React.Fragment>
  );
};
