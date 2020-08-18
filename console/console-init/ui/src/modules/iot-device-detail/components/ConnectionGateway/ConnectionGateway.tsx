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
  CardTitle
} from "@patternfly/react-core";
import { OutlinedQuestionCircleIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "aphrodite";

export interface IConnectionGatewayProps {
  deviceList?: string[];
  gatewayGroups?: string[];
}

const styles = StyleSheet.create({
  card_body: {
    marginBottom: "1.25rem"
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
        <Title
          id="connection-gateway-devices-title"
          headingLevel="h1"
          size="2xl"
        >
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
      <CardBody>
        {deviceList && deviceList.length > 0 && (
          <>
            <Text>Gateway Devices</Text>
            <Flex>
              {deviceList.map((deviceId: string) => {
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
          </>
        )}
        <br />
        {gatewayGroups && gatewayGroups.length > 0 && (
          <>
            <Text>Gateway Groups</Text>
            <Flex>
              {gatewayGroups.map((group: string) => {
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
          </>
        )}
      </CardBody>
    </Card>
  );
};
