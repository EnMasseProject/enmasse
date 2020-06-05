/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";

import {
  Card,
  CardBody,
  TextContent,
  Flex,
  FlexItem,
  Divider
} from "@patternfly/react-core";
import {
  CheckCircleIcon,
  PendingIcon,
  InProgressIcon,
  OutlinedTimesCircleIcon
} from "@patternfly/react-icons";
import { IProjectCount } from "./ProjectHeaderCard";

interface IProjectCardProps {
  count: IProjectCount;
  label: string;
  labelShort?: string;
}
const ProjectCard: React.FunctionComponent<IProjectCardProps> = ({
  count,
  label,
  labelShort
}) => {
  const { configuring, pending, failed, total } = count;
  return (
    <Card isHoverable>
      <CardBody>
        <TextContent>
          <span style={{ fontSize: 24 }}>
            <b>{total}</b>
          </span>
          &nbsp; &nbsp;{label}
          <br />( {labelShort} )
          <br />
          <Divider />
          <Flex>
            {(!configuring || configuring === 0) &&
              (!failed || failed === 0) &&
              (!pending || pending === 0) && (
                <FlexItem>
                  <CheckCircleIcon color="var(--pf-global--palette--green-400)" />
                </FlexItem>
              )}
            <FlexItem>
              {failed && failed > 0 ? (
                <>
                  <OutlinedTimesCircleIcon color="var(--pf-global--danger-color--100)" />{" "}
                  {failed}
                </>
              ) : (
                ""
              )}
            </FlexItem>
            <FlexItem>
              {configuring && configuring > 0 ? (
                <>
                  <InProgressIcon color="var(--pf-global--icon--Color--light)" />{" "}
                  {configuring}
                </>
              ) : (
                ""
              )}
            </FlexItem>
            <FlexItem>
              {pending && pending > 0 ? (
                <>
                  <PendingIcon color="var(--pf-global--icon--Color--light)" />{" "}
                  {pending}
                </>
              ) : (
                ""
              )}
            </FlexItem>
          </Flex>
        </TextContent>
      </CardBody>
    </Card>
  );
};

export { ProjectCard };
