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
  Button
} from "@patternfly/react-core";

interface DeviceInfo {
  id: string;
  name: string;
}

export interface DeviceInfoGatewaysProps {
  deviceList?: DeviceInfo[];
}

export const DeviceInfoGateways: React.FC<DeviceInfoGatewaysProps> = ({
  deviceList
}) => {
  const DeviceNotFound = () => (
    <div>
      There is no gateways for this device. This device is connected to the
      cloud directly.
    </div>
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
            deviceList.map((device: DeviceInfo) => {
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
