import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useParams } from "react-router";
import {
  Grid,
  GridItem,
  Card,
  CardBody,
  Title,
  Text,
  TextVariants,
  CardTitle
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
export interface IConnectionGatewayProps {
  deviceList?: any;
  deviceGroup?: any;
}
const styles = StyleSheet.create({
  card_body: {
    marginBottom: 20
  }
});

export const ConnectionGateway: React.FC<IConnectionGatewayProps> = ({
  deviceList,
  deviceGroup
}) => {
  const { projectname, namespace } = useParams();

  return (
    <Card className={css(styles.card_body)}>
      <CardTitle>
        <Title id="device-info-gateways-title" headingLevel="h1" size="2xl">
          Connection Gateways
        </Title>
      </CardTitle>
      <CardBody>
        <Grid>
          {deviceList.length > 0 && (
            <GridItem>
              <text>Gateway Devices</text>
              <br />
            </GridItem>
          )}
          <br />
          {deviceList &&
            deviceList.map((deviceId: string) => {
              return (
                <GridItem span={2} key={deviceId}>
                  <Link
                    id="device-info-id-link"
                    to={`/iot-projects/${namespace}/${projectname}/devices/${deviceId}/device-info`}
                  >
                    {deviceId}
                  </Link>
                </GridItem>
              );
            })}
          <br />
          <br />
          {deviceGroup.length > 0 && (
            <GridItem>
              <text>Gateway groups</text>
              <br />
            </GridItem>
          )}
          <br />
          {deviceGroup &&
            deviceGroup.map((deviceId: string) => {
              return (
                <GridItem span={2} key={deviceId}>
                  <Link
                    id="device-info-id-link"
                    to={`/iot-projects/${namespace}/${projectname}/devices/${deviceId}/device-info`}
                  >
                    {deviceId}
                  </Link>
                </GridItem>
              );
            })}
          {deviceList?.length <= 0 && deviceGroup?.length <= 0 && (
            <Text component={TextVariants.p} id="device-info-no-gateways-text">
              There are no gateways for this device. This device is connected to
              the cloud directly.
            </Text>
          )}
        </Grid>
      </CardBody>
    </Card>
  );
};
