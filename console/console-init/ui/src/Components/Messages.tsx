import * as React from "react";
import {
  ArrowAltCircleLeftIcon,
  ArrowAltCircleRightIcon
} from "@patternfly/react-icons";

interface IMessagesProps {
  count: number;
  column: string;
  status: string;
}

export const Messages: React.FunctionComponent<IMessagesProps> = message => {
  //TBD: color and icons to be set when designs are finalized.
  const iconColor = message.status === "running" ? "blue" : "grey";
  const icon =
    message.column === "MessagesIn" ? (
      <ArrowAltCircleRightIcon color={iconColor} key="icon" />
    ) : (
      <ArrowAltCircleLeftIcon color={iconColor} key="icon" />
    );

  return (
    <React.Fragment>
      {icon} {message.count}
    </React.Fragment>
  );
};
