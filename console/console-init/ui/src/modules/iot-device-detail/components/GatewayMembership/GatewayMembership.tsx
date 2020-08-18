/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Link } from "react-router-dom";
import { useParams } from "react-router";
import {
  Flex,
  FlexItem,
  Card,
  Popover,
  Button,
  CardBody,
  Title,
  CardTitle
} from "@patternfly/react-core";
import { OutlinedQuestionCircleIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "aphrodite";

export interface IGatewayMembershipProps {
  memberOf?: string[];
}

const styles = StyleSheet.create({
  card_body: {
    marginBottom: "1.25rem"
  },
  flex_item_margin: {
    marginLeft: "0.625rem",
    paddingLeft: "0"
  },
  tooltip_margin: {
    paddingRight: "3.125rem"
  }
});

export const GatewayMembership: React.FC<IGatewayMembershipProps> = ({
  memberOf
}) => {
  const { projectname, namespace } = useParams();

  return (
    <Card className={css(styles.card_body)}>
      <CardTitle>
        <Title
          className={css(styles.tooltip_margin)}
          id="gateway-membership-title"
          headingLevel="h1"
          size="2xl"
        >
          Gateway Group Membership &nbsp;&nbsp;
          <Popover
            enableFlip={false}
            bodyContent={<div>Gateway groups to which the device belongs</div>}
            aria-label="gateway group membership info popover"
            closeBtnAriaLabel="close gateway membership info popover"
          >
            <Button
              variant="link"
              id="gateway-membership-help-button"
              icon={<OutlinedQuestionCircleIcon />}
            ></Button>
          </Popover>
        </Title>
      </CardTitle>

      <CardBody className={css(styles.flex_item_margin)}>
        <Flex>
          <br />
          {memberOf &&
            memberOf.map((group: string) => {
              return (
                <FlexItem span={2} key={group}>
                  <Link
                    id="device-info-id-link"
                    to={`/iot-projects/${namespace}/${projectname}/devices/${group}/device-info`}
                  >
                    {group}
                  </Link>
                </FlexItem>
              );
            })}
        </Flex>
      </CardBody>
    </Card>
  );
};
