/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React from "react";
import { IDeviceProp } from "modules/iot-device/containers/ReviewDeviceContainer";
import {
  Title,
  Text,
  GridItem,
  Grid,
  Split,
  SplitItem
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { CredentialsView } from "modules/iot-device-detail";
interface IReviewDeviceProps {
  device?: IDeviceProp;
  title?: string;
}

const style = StyleSheet.create({
  font_size_20: {
    fontSize: 20
  },
  split_right_margin: {
    marginRight: 15
  }
});

const ReviewDevice: React.FunctionComponent<IReviewDeviceProps> = ({
  device,
  title
}) => {
  const getDeviceInformation = () => {
    const { deviceId, status, metadata } = device?.deviceInformation || {};
    return (
      <>
        <Text className={css(style.font_size_20)}>Device Information</Text>
        <br />
        {deviceId && (
          <>
            <Title size="lg" headingLevel="h5">
              Device ID
            </Title>
            {deviceId}
            <br />
            <br />
          </>
        )}
        {status !== undefined && (
          <>
            <Title size="lg" headingLevel="h5">
              Status
            </Title>
            {status ? "Enabled" : "Disabled"}
            <br />
            <br />
          </>
        )}
        {metadata !== undefined && (
          <>
            <Title size="lg" headingLevel="h5">
              MetaData
            </Title>
            {/*TODO: {metadata} */}
            --
            <br />
            <br />
          </>
        )}
      </>
    );
  };

  const getConnectionType = () => {
    const { gateways, credentials } = device || {};

    let connType: string = "";
    const viaGateway = gateways
      ? (gateways.gateways && gateways.gateways.length > 0) ||
        (gateways.gatewayGroups && gateways.gatewayGroups.length > 0)
      : false;
    if (credentials && credentials.length > 0 && !viaGateway) {
      connType = "Connected directly";
    } else if (viaGateway && !credentials?.length) {
      connType = "Connected via gateways";
    } else {
      connType = "N/A";
    }

    return (
      <>
        <Text className={css(style.font_size_20)}>Connection Type</Text>
        <br />
        {connType}
        <br />
        <br />
      </>
    );
  };

  const getGateways = () => {
    const { gateways } = device?.gateways || {};
    return (
      <>
        <Text className={css(style.font_size_20)}>Gateways</Text>
        <br />
        <Split>
          {gateways && gateways?.length > 0
            ? gateways.map(gateway => (
                <SplitItem
                  key={gateway}
                  className={css(style.split_right_margin)}
                >
                  {gateway}
                </SplitItem>
              ))
            : "--"}
        </Split>
        <br />
      </>
    );
  };

  const getGatewayGroups = () => {
    const { gatewayGroups } = device?.gateways || {};
    return (
      <>
        <Text className={css(style.font_size_20)}>Gateway groups</Text>
        <br />
        <Split>
          {gatewayGroups && gatewayGroups?.length > 0
            ? gatewayGroups.map(gatewayGroup => (
                <SplitItem
                  key={gatewayGroup}
                  className={css(style.split_right_margin)}
                >
                  {gatewayGroup}
                </SplitItem>
              ))
            : "--"}
        </Split>
        <br />
      </>
    );
  };

  const getMemberOfs = () => {
    const { memberOf } = device || {};
    return (
      <>
        <Text className={css(style.font_size_20)}>Gateway Members</Text>
        <br />
        <Split>
          {memberOf && memberOf?.length > 0
            ? memberOf.map(member => (
                <SplitItem
                  key={member}
                  className={css(style.split_right_margin)}
                >
                  {member}
                </SplitItem>
              ))
            : "--"}
        </Split>
        <br />
      </>
    );
  };

  const getCredentials = () => {
    const { credentials } = device || {};
    return (
      <>
        <Text className={css(style.font_size_20)}>Credentials</Text>
        <br />
        {credentials && (
          <CredentialsView
            id={"review-device-crdentials-list"}
            credentials={credentials}
            enableActions={false}
          />
        )}
      </>
    );
  };

  const { deviceInformation, connectionType, credentials, gateways, memberOf } =
    device || {};
  return (
    <>
      <Grid>
        <GridItem span={8}>
          {title && title.trim() !== "" && (
            <>
              <Title size="2xl" headingLevel="h2">
                {title}
              </Title>
              <br />
            </>
          )}
          <br />
          {deviceInformation && getDeviceInformation()}
          {connectionType && getConnectionType()}
          {credentials && credentials.length > 0 && getCredentials()}
          {gateways?.gateways && getGateways()}
          {gateways?.gatewayGroups && getGatewayGroups()}
          {memberOf && getMemberOfs()}
        </GridItem>
      </Grid>
    </>
  );
};

export { ReviewDevice };
