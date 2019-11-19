import * as React from "react";

interface IMessagesProps {
  count: any;
  column: string;
  status?: string;
  isReady: boolean;
}

export const Messages: React.FunctionComponent<IMessagesProps> = message => {
  return <React.Fragment>{message.count}</React.Fragment>;
};
