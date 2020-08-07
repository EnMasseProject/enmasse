import React, { useState } from "react";
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
  memberGroup?: any;
}
const styles = StyleSheet.create({
  card_body: {
    marginBottom: 20
  },
  flex_item_margin: {
    marginLeft: 10,
    paddingLeft: 0
  },
  tooltip_margin: {
    paddingRight: 50
  }
});

export const GatewayMembership: React.FC<IGatewayMembershipProps> = ({
  memberGroup
}) => {
  const { projectname, namespace } = useParams();

  return (
    <Card className={css(styles.card_body)}>
      <CardTitle>
        <Title
          className={css(styles.tooltip_margin)}
          id="device-info-gateways-title"
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
              icon={<OutlinedQuestionCircleIcon />}
            ></Button>
          </Popover>
        </Title>
      </CardTitle>

      <CardBody className={css(styles.flex_item_margin)}>
        <Flex>
          <br />
          {memberGroup &&
            memberGroup.map((deviceId: string) => {
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
