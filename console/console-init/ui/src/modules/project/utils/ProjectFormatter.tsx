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
import { Flex, FlexItem, Label } from "@patternfly/react-core";
import { kFormatter } from "utils";
import { StyleSheet, css } from "aphrodite";
import { ProjectTypes } from "./constant";

const style = StyleSheet.create({
  red_color: {
    color: "var(--pf-global--palette--red-100)"
  }
});
interface IProjectTypeProps {
  projectType: string;
}

interface IProjectStatusProps {
  phase: string;
  messages?: string[];
}
interface IProjectErrorProps {
  errorCount?: number;
  errorMessages?: string[];
}
interface IProjectEntitiesProps {
  projectType: ProjectTypes;
  addressCount?: number;
  connectionCount?: number;
  deviceCount?: number;
  activeCount?: number;
}
interface IProjectErrorMessagesProps {
  messages?: string[];
}

const getStatusIconByPhase = (phase: string) => {
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
const formatString = (string?: string) => {
  if (string) {
    const formattedString =
      string[0].toUpperCase() + "" + string.substring(1, string.length);
    return formattedString;
  }
};
const ProjectTypeLabel: React.FunctionComponent<IProjectTypeProps> = ({
  projectType
}) => {
  const color = projectType === ProjectTypes.MESSAGING ? "green" : "blue";
  return <Label color={color}>{projectType}</Label>;
};

const ProjectError: React.FunctionComponent<IProjectErrorProps> = ({
  errorCount,
  errorMessages
}) => {
  return (
    <>
      {errorCount ? (
        errorCount >= 25 ? (
          <span className={css(style.red_color)}>{errorCount}%</span>
        ) : (
          errorCount + "%"
        )
      ) : (
        "-"
      )}
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
  addressCount = 0,
  connectionCount = 0,
  deviceCount = 0,
  activeCount = 0
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
      {getStatusIconByPhase(phase)}
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
  ProjectTypeLabel,
  getStatusIconByPhase,
  ProjectErrorMessages
};
