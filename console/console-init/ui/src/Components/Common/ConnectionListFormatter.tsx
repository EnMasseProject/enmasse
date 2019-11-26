import * as React from "react";
import { LockOpenIcon, LockIcon } from "@patternfly/react-icons";
import { IMetrics } from "src/Types/ResponseTypes";

interface ProtocolIcon {
  protocol: string;
}

const protocolIconToDisplay = (protocol: string) => {
  switch (protocol.toLowerCase()) {
    case "amqp":
      return <LockIcon />;
    default:
      return <LockOpenIcon />;
  }
};

export const ConnectionProtocolFormat: React.FunctionComponent<ProtocolIcon> = ({
  protocol
}) => {
  return (
    <>
      {protocol.toUpperCase()} {protocolIconToDisplay(protocol)}
    </>
  );
};

export const getFilteredValue = (object: IMetrics[], value: string) => {
  const filtered = object.filter(obj => obj.Name === value);
  if (filtered.length > 0) {
    return filtered[0].Value;
  }
  return 0;
};
