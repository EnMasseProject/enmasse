import * as React from "react";
import { LockOpenIcon, LockIcon } from "@patternfly/react-icons";

interface ProtocolIcon {
  protocol: string;
}

const protocolIconToDisplay = (protocol: string) => {
  switch (protocol) {
    case "amqp"||"AMQP":
      return <LockIcon />;
    default:
      return <LockOpenIcon />;
  }
};

export const ConnectionProtocolFormat: React.FunctionComponent<
ProtocolIcon
> = ({ protocol }) => {
  return (
    <>
      {protocol.toUpperCase()} {protocolIconToDisplay(protocol)}
    </>
  );
};