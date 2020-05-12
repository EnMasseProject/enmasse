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

interface IDeviceInfo {
  id: string;
  name: string;
}

export interface IDeviceInfoGatewaysProps {
  deviceList?: IDeviceInfo[];
}

export const DeviceInfoGateways: React.FC<IDeviceInfoGatewaysProps> = ({
  deviceList
}) => {
  const DeviceNotFound = () => (
    <Text component={TextVariants.p}>
      There is no gateways for this device. This device is connected to the
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
            deviceList.map((device: IDeviceInfo) => {
              const { name, id } = device;
              return (
                <GridItem span={2} key={id}>
                  <Button variant="link" isInline>
                    {name}
                  </Button>
                </GridItem>
              );
            })}
          {!(deviceList && deviceList.length > 0) && <DeviceNotFound />}
        </Grid>
      </CardBody>
    </Card>
  );
};
