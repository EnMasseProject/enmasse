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

const Messages: React.FunctionComponent<IMessagesProps> = message => {
  const icon =
    message.column == "MessagesIn" ? (
      <ArrowAltCircleRightIcon key="icon" />
    ) : (
      <ArrowAltCircleLeftIcon key="icon" />
    );

  //TODO: Change color of icon based on status
  return (
    <React.Fragment>
      {icon} {message.count}
    </React.Fragment>
  );
};

export default Messages;
