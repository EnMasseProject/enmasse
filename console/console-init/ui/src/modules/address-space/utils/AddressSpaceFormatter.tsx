/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  CheckCircleIcon,
  InProgressIcon,
  ExclamationCircleIcon
} from "@patternfly/react-icons";
import { Badge } from "@patternfly/react-core";
import { getType } from "utils";

interface AddressSpaceTypeProps {
  type: string;
}

interface AddressSpaceStatusProps {
  phase: string;
  messages: Array<string>;
}

const AddressSpaceIcon = () => {
  return (
    <Badge
      style={{
        backgroundColor: "#EC7A08",
        fontSize: "var(--pf-c-table-cell--FontSize)"
      }}
    >
      AS
    </Badge>
  );
};

const statusToDisplay = (phase: string) => {
  let icon;
  switch (phase.toLowerCase()) {
    case "active":
      icon = <CheckCircleIcon color="green" />;
      break;
    case "configuring":
      icon = <InProgressIcon />;
      break;
    case "pending":
      icon = <ExclamationCircleIcon color="red" />;
      break;
    case "":
      icon = <InProgressIcon />;
      break;
  }
  return (
    <span>
      {icon}&nbsp;{phase.trim() !== "" ? phase : "Configuring"}
    </span>
  );
};

const AddressSpaceType: React.FunctionComponent<AddressSpaceTypeProps> = ({
  type
}) => {
  return <>{getType(type)}</>;
};

const AddressSpaceStatus: React.FunctionComponent<AddressSpaceStatusProps> = ({
  phase,
  messages
}) => {
  return (
    <>
      {statusToDisplay(phase)}
      {messages.map(message => (
        <>
          <br />
          {message}
        </>
      ))}
    </>
  );
};

export {
  AddressSpaceStatus,
  AddressSpaceType,
  statusToDisplay,
  AddressSpaceIcon
};
