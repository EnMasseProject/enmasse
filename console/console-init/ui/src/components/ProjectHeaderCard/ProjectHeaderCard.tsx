/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Card, CardBody, TextContent, Gallery } from "@patternfly/react-core";
import { ProjectCard } from "./ProjectCard";

export interface IProjectCount {
  total: number;
  failed?: number;
  active?: number;
  pending?: number;
  configuring?: number;
}

interface IProjectHeaderCardProps {
  totalProject: number;
  ioTCount?: IProjectCount;
  msgCount?: IProjectCount;
}

const ProjectHeaderCard: React.FunctionComponent<IProjectHeaderCardProps> = ({
  totalProject,
  ioTCount,
  msgCount,
}) => {
  return (
    <Gallery gutter="sm">
      <Card isHoverable>
        <CardBody style={{ textAlign: "center", marginTop: 10 }}>
          <TextContent>
            <span style={{ fontSize: 24 }}>
              <b>{totalProject}</b>
            </span>
            <br />
            <span style={{ fontSize: 22 }}>Total</span>
          </TextContent>
        </CardBody>
      </Card>
      {ioTCount && (
        <ProjectCard
          count={ioTCount}
          labelShort={"IoT"}
          label={"IoT Projects"}
        />
      )}
      {msgCount && (
        <ProjectCard
          count={msgCount}
          labelShort={"Msg"}
          label={"Messaging Projects"}
        />
      )}
    </Gallery>
  );
};
export { ProjectHeaderCard };
