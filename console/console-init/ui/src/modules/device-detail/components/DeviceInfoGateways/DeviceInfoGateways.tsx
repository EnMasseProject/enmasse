/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Grid,
  GridItem,
  Card,
  CardBody,
  CardHeader,
  Title,
  Button,
  Text,
  TextVariants
} from "@patternfly/react-core";
import { Link } from "react-router-dom";

export interface IDeviceInfoGatewaysProps {
  deviceList?: string[];
}

export const DeviceInfoGateways: React.FC<IDeviceInfoGatewaysProps> = ({
  deviceList
}) => {
  const DeviceNotFound = () => (
    <Text component={TextVariants.p}>
      There are no gateways for this device. This device is connected to the
      cloud directly.
    </Text>
  );

  return (
    <Card>
      <CardHeader>
        <Title id="di-header-title" headingLevel="h1" size="2xl">
          Via gateways
        </Title>
      </CardHeader>
      <CardBody>
        <Grid>
          {deviceList &&
            deviceList.map((device: string) => {
              return (
                <GridItem span={2} key={device}>
                  {/**  TODO:add link redirect url*/}
                  <Link to={"/"}>{device}</Link>
                </GridItem>
              );
            })}
          {!(deviceList && deviceList.length > 0) && <DeviceNotFound />}
        </Grid>
      </CardBody>
    </Card>
  );
};
