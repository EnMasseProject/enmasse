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
  return (
    <Card isHoverable>
      <CardBody>
        <TextContent>
          <span style={{ fontSize: 24 }}>
            <b>{count.total}</b>
          </span>
          &nbsp; &nbsp;{label}
          <br />( {labelShort} )
          <br />
          <Divider />
          <Flex>
            {(!count.configuring || count.configuring === 0) &&
              (!count.failed || count.failed === 0) &&
              (!count.pending || count.pending === 0) && (
                <FlexItem>
                  <CheckCircleIcon color="var(--pf-global--palette--green-400)" />
                </FlexItem>
              )}
            <FlexItem>
              {count.failed && count.failed > 0 ? (
                <>
                  <OutlinedTimesCircleIcon color="var(--pf-global--danger-color--100)" />{" "}
                  {count.failed}
                </>
              ) : (
                ""
              )}
            </FlexItem>
            <FlexItem>
              {count.configuring && count.configuring > 0 ? (
                <>
                  <InProgressIcon color="var(--pf-global--icon--Color--light)" />{" "}
                  {count.configuring}
                </>
              ) : (
                ""
              )}
            </FlexItem>
            <FlexItem>
              {count.pending && count.pending > 0 ? (
                <>
                  <PendingIcon color="var(--pf-global--icon--Color--light)" />{" "}
                  {count.pending}
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
