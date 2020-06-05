/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Card, CardBody, TextContent, Gallery } from "@patternfly/react-core";
import { ProjectCard } from "./ProjectCard";
import { StyleSheet, css } from "@patternfly/react-styles";

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

const style = StyleSheet.create({
  totol_card_body: {
    textAlign: "center",
    marginTop: 10
  },
  total_count_font: { fontSize: 24 },
  total_label_font: { fontSize: 22 }
});

const ProjectHeaderCard: React.FunctionComponent<IProjectHeaderCardProps> = ({
  totalProject,
  ioTCount,
  msgCount
}) => {
  return (
    <Gallery gutter="sm">
      <Card isHoverable>
        <CardBody className={css(style.totol_card_body)}>
          <TextContent>
            <span className={css(style.total_count_font)}>
              <b>{totalProject}</b>
            </span>
            <br />
            <span className={css(style.total_label_font)}>Total</span>
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
