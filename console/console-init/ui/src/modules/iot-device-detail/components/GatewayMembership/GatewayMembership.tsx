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
  memberOf?: any;
}
const styles = StyleSheet.create({
  card_body: {
    marginBottom: "20rem"
  },
  flex_item_margin: {
    marginLeft: "10rem",
    paddingLeft: "0rem"
  },
  tooltip_margin: {
    paddingRight: "50rem"
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
            memberOf.map((deviceId: string) => {
              return (
                <FlexItem span={2} key={deviceId}>
                  <Link
                    id="device-info-id-link"
                    to={`/iot-projects/${namespace}/${projectname}/devices/${deviceId}/device-info`}
                  >
                    {deviceId}
                  </Link>
                </FlexItem>
              );
            })}
        </Flex>
      </CardBody>
    </Card>
  );
};
