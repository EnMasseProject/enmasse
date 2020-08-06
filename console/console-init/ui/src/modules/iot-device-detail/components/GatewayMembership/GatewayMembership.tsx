import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useParams } from "react-router";
import {
  Grid,
  GridItem,
  Card,
  CardBody,
  Title,
  CardTitle
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";

export interface IGatewayMembershipProps {
  memberGroup?: any;
}
const styles = StyleSheet.create({
  card_body: {
    marginBottom: 20
  }
});

export const GatewayMembership: React.FC<IGatewayMembershipProps> = ({
  memberGroup
}) => {
  const { projectname, namespace } = useParams();
  return (
    <Card className={css(styles.card_body)}>
      <CardTitle>
        <Title id="device-info-gateways-title" headingLevel="h1" size="2xl">
          Gateway Group Membership
        </Title>
      </CardTitle>
      <CardBody>
        <Grid>
          <br />
          {memberGroup &&
            memberGroup.map((deviceId: string) => {
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
        </Grid>
      </CardBody>
    </Card>
  );
};
