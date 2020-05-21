/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  CheckCircleIcon,
  InProgressIcon,
  ErrorCircleOIcon,
  PendingIcon
} from "@patternfly/react-icons";
import { Flex, FlexItem } from "@patternfly/react-core";
import { kFormatter } from "utils";
import { StyleSheet, css } from "@patternfly/react-styles";
import { ProjectTypes } from "./constant";

const style = StyleSheet.create({
  red_color: {
    color: "var(--pf-global--palette--red-100)"
  }
});
interface IProjectTypePlanProps {
  type: string;
  plan?: string;
}

interface IProjectStatusProps {
  phase: string;
  messages?: Array<string>;
}
interface IProjectErrorProps {
  errorCount?: number;
  errorMessages?: Array<string>;
}
interface IProjectEntitiesProps {
  projectType: ProjectTypes;
  addressCount?: number;
  connectionCount?: number;
  deviceCount?: number;
  activeCount?: number;
}
interface IProjectErrorMessagesProps {
  messages?: Array<string>;
}

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
      icon = <PendingIcon />;
      break;
    case "failed":
      icon = <ErrorCircleOIcon color="red" />;
      break;
    default:
      icon = <PendingIcon />;
      break;
  }
  return (
    <span>
      {icon}&nbsp;{phase.trim() !== "" ? phase : "Pending"}
    </span>
  );
};

const ProjectTypePlan: React.FunctionComponent<IProjectTypePlanProps> = ({
  type,
  plan
}) => {
  return (
    <>
      {type} {plan}
    </>
  );
};

const ProjectError: React.FunctionComponent<IProjectErrorProps> = ({
  errorCount,
  errorMessages
}) => {
  return (
    <>
      {errorCount &&
        (errorCount >= 25 ? (
          <span className={css(style.red_color)}>{errorCount}%</span>
        ) : (
          errorCount + "%"
        ))}
      {errorMessages && (
        <>
          {errorCount && <br />}
          <ProjectErrorMessages messages={errorMessages} />
        </>
      )}
    </>
  );
};

const ProjectEntities: React.FunctionComponent<IProjectEntitiesProps> = ({
  projectType,
  addressCount,
  connectionCount,
  deviceCount,
  activeCount
}) => {
  return (
    <>
      {projectType === ProjectTypes.MESSAGING ? (
        <Flex>
          <FlexItem>
            {addressCount || 0}
            <br />
            Addresses
          </FlexItem>
          <FlexItem span={6}>
            {connectionCount || 0}
            <br />
            Connections
          </FlexItem>
        </Flex>
      ) : (
        <Flex>
          <FlexItem span={6}>
            {kFormatter(deviceCount || 0)}
            <br />
            Devices
          </FlexItem>
          <FlexItem span={6}>
            {kFormatter(activeCount || 0)}
            <br />
            Active
          </FlexItem>
        </Flex>
      )}
    </>
  );
};
const ProjectStatus: React.FunctionComponent<IProjectStatusProps> = ({
  phase,
  messages
}) => {
  return (
    <>
      {statusToDisplay(phase)}
      {messages &&
        messages.map(message => (
          <>
            <br />
            {message}
          </>
        ))}
    </>
  );
};

const ProjectErrorMessages: React.FunctionComponent<IProjectErrorMessagesProps> = ({
  messages
}) => {
  return (
    <>
      {messages &&
        messages.map(message => (
          <>
            {message}
            <br />
          </>
        ))}
    </>
  );
};

export {
  ProjectStatus,
  ProjectError,
  ProjectEntities,
  ProjectTypePlan,
  statusToDisplay,
  ProjectErrorMessages
};
