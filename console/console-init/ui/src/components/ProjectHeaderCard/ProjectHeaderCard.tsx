/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Card,
  PageSection,
  CardBody,
  TextContent,
  Flex,
  FlexItem,
  Divider,
  Gallery,
  CardHeader,
  Page
} from "@patternfly/react-core";
import {
  CheckCircleIcon,
  PendingIcon,
  InProgressIcon,
  OutlinedTimesCircleIcon
} from "@patternfly/react-icons";

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
  msgCount
}) => {
  return (
    <PageSection>
      <Gallery gutter="sm">
        <Card isHoverable>
          <CardHeader>
            <TextContent>
              <span style={{ fontSize: 24 }}>
                <b>{totalProject}</b>
              </span>
              <br />
              <span style={{ fontSize: 22 }}>Total</span>
            </TextContent>
          </CardHeader>
        </Card>
        {ioTCount && (
          <Card isHoverable>
            <CardBody>
              <TextContent>
                <span style={{ fontSize: 24 }}>
                  <b>{ioTCount.total}</b>
                </span>
                &nbsp; &nbsp;IoT Project (IoT)
                <br />
                <Divider />
                <Flex>
                  {(!ioTCount.configuring || ioTCount.configuring == 0) &&
                    (!ioTCount.failed || ioTCount.failed == 0) &&
                    (!ioTCount.pending || ioTCount.pending == 0) && (
                      <FlexItem>
                        <CheckCircleIcon color="var(--pf-global--palette--green-400)" />
                      </FlexItem>
                    )}
                  <FlexItem>
                    {ioTCount.failed && ioTCount.failed > 0 ? (
                      <>
                        <OutlinedTimesCircleIcon color="var(--pf-global--danger-color--100)" />{" "}
                        {ioTCount.failed}
                      </>
                    ) : (
                      ""
                    )}
                  </FlexItem>
                  <FlexItem>
                    {ioTCount.configuring && ioTCount.configuring > 0 ? (
                      <>
                        <InProgressIcon color="var(--pf-global--icon--Color--light)" />{" "}
                        {ioTCount.configuring}
                      </>
                    ) : (
                      ""
                    )}
                  </FlexItem>
                  <FlexItem>
                    {ioTCount.pending && ioTCount.pending > 0 ? (
                      <>
                        <PendingIcon color="var(--pf-global--icon--Color--light)" />{" "}
                        {ioTCount.pending}
                      </>
                    ) : (
                      ""
                    )}
                  </FlexItem>
                </Flex>
              </TextContent>
            </CardBody>
          </Card>
        )}
        {msgCount && (
          <Card isHoverable>
            <CardBody>
              <TextContent>
                <span style={{ fontSize: 24 }}>
                  <b>{msgCount.total}</b>
                </span>
                &nbsp; &nbsp;Messaging Project (MSG)
                <br />
                <Divider />
                <Flex>
                  {(!msgCount.configuring || msgCount.configuring == 0) &&
                    (!msgCount.failed || msgCount.failed == 0) &&
                    (!msgCount.pending || msgCount.pending == 0) && (
                      <FlexItem>
                        <CheckCircleIcon color="var(--pf-global--palette--green-400)" />
                      </FlexItem>
                    )}
                  <FlexItem>
                    {msgCount.failed && msgCount.failed > 0 ? (
                      <>
                        <OutlinedTimesCircleIcon color="var(--pf-global--danger-color--100)" />{" "}
                        {msgCount.failed}
                      </>
                    ) : (
                      ""
                    )}
                  </FlexItem>
                  <FlexItem>
                    {msgCount.configuring && msgCount.configuring > 0 ? (
                      <>
                        <InProgressIcon color="var(--pf-global--icon--Color--light)" />{" "}
                        {msgCount.configuring}
                      </>
                    ) : (
                      ""
                    )}
                  </FlexItem>
                  <FlexItem>
                    {msgCount.pending && msgCount.pending > 0 ? (
                      <>
                        <PendingIcon color="var(--pf-global--icon--Color--light)" />{" "}
                        {msgCount.pending}
                      </>
                    ) : (
                      ""
                    )}
                  </FlexItem>
                </Flex>
              </TextContent>
            </CardBody>
          </Card>
        )}
      </Gallery>
    </PageSection>
  );
};
export { ProjectHeaderCard };
