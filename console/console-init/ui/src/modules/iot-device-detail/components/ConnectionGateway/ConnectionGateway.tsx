/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useParams } from "react-router";
import {
  Card,
  CardBody,
  Title,
  Popover,
  Button,
  CardTitle
} from "@patternfly/react-core";
import { OutlinedQuestionCircleIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "aphrodite";
import { GatewayItems } from "modules/iot-device-detail/components/ConnectionGateway";

export interface IConnectionGatewayProps {
  deviceList?: string[];
  gatewayGroups?: string[];
}

const styles = StyleSheet.create({
  card_body: {
    marginBottom: "1.25rem"
  },
  popover_btn: {
    color: "var(--pf-global--palette--black-1000)",
    paddingLeft: "0"
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
              className={css(styles.popover_btn)}
              icon={<OutlinedQuestionCircleIcon />}
            ></Button>
          </Popover>
        </Title>
      </CardTitle>
      <CardBody>
        {deviceList && deviceList.length > 0 && (
          <GatewayItems
            title="Gateway Devices"
            gateways={deviceList}
            projectname={projectname}
            namespace={namespace}
          />
        )}
        <br />
        {gatewayGroups && gatewayGroups.length > 0 && (
          <GatewayItems
            title="Gateway Groups"
            gateways={gatewayGroups}
            projectname={projectname}
            namespace={namespace}
          />
        )}
      </CardBody>
    </Card>
  );
};
