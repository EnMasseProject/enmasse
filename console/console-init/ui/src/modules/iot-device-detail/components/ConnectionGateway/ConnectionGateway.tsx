/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React from "react";
import { Link } from "react-router-dom";
import { useParams } from "react-router";
import {
  Card,
  Flex,
  FlexItem,
  CardBody,
  Title,
  Text,
  Popover,
  Button,
  TextVariants,
  CardTitle
} from "@patternfly/react-core";
import { OutlinedQuestionCircleIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "aphrodite";
export interface IConnectionGatewayProps {
  deviceList?: any;
  gatewayGroups?: any;
}
const styles = StyleSheet.create({
  card_body: {
    marginBottom: "20rem"
  },
  text_margin: {
    marginLeft: "25rem"
  }
});

export const ConnectionGateway: React.FC<IConnectionGatewayProps> = ({
  deviceList,
  gatewayGroups
}) => {
  const { projectname, namespace } = useParams();

  return (
    <Card className={css(styles.card_body)}>
      <CardTitle>
        <Title id="device-info-gateways-title" headingLevel="h1" size="2xl">
          Connection Gateways&nbsp;&nbsp;
          <Popover
            enableFlip={false}
            bodyContent={<div>Gateway Groups to which the device belongs</div>}
            aria-label="Add gateway devices info popover"
            closeBtnAriaLabel="Close Gateway Devices info popover"
          >
            <Button
              variant="link"
              id="connection-gateway-help-button"
              icon={<OutlinedQuestionCircleIcon />}
            ></Button>
          </Popover>
        </Title>
      </CardTitle>
      {deviceList.length > 0 && (
        <text className={css(styles.text_margin)}>Gateway Devices</text>
      )}
      <CardBody>
        <Flex>
          {deviceList &&
            deviceList.map((deviceId: string) => {
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
        <br />
        {deviceList.length > 0 && <text>Gateway Groups</text>}
        <br />
        <Flex>
          {gatewayGroups &&
            gatewayGroups.map((deviceId: string) => {
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
