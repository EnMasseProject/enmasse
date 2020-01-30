/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

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
  if (object && object != null) {
    const filtered = object.filter(
      obj => obj && obj.name && obj.name === value
    );
    if (filtered.length > 0) {
      return filtered[0].value;
    }
  }
  return "";
};
