import * as React from "react";
import { LockOpenIcon, LockIcon } from "@patternfly/react-icons";
import { IMetrics } from "src/Types/ResponseTypes";

interface ProtocolIcon {
  protocol: string;
  encrypted: boolean;
}

const protocolIconToDisplay = (encrypted: boolean) => {
  if (encrypted) return <LockIcon />;
  else return <LockOpenIcon />;
};

export const ConnectionProtocolFormat: React.FunctionComponent<ProtocolIcon> = ({
  protocol,
  encrypted
}) => {
  return (
    <>
      {protocol.toUpperCase()} {protocolIconToDisplay(encrypted)}
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
