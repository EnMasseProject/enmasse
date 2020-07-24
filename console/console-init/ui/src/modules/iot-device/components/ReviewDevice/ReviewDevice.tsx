/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React from "react";
import { IDeviceProp } from "modules/iot-device/containers/ReviewDeviceContainer";
import { Title, Text, GridItem, Grid } from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { CredentialsView } from "modules/iot-device-detail";
interface IReviewDeviceProps {
  device?: IDeviceProp;
  title?: string;
}

const style = StyleSheet.create({
  font_size_20: {
    fontSize: 20
  }
});

const ReviewDevice: React.FunctionComponent<IReviewDeviceProps> = ({
  device,
  title
}) => {
  const getDeviceInformation = () => {
    const { deviceInformation } = device || {};
    return (
      <>
        <Text className={css(style.font_size_20)}>Device Information</Text>
        <br />
        {deviceInformation?.deviceId && (
          <>
            <Title size="lg" headingLevel="h5">
              Device ID
            </Title>
            {deviceInformation.deviceId}
            <br />
            <br />
          </>
        )}
        {deviceInformation?.status !== undefined && (
          <>
            <Title size="lg" headingLevel="h5">
              Status
            </Title>
            {deviceInformation.status ? "Enabled" : "Disabled"}
            <br />
            <br />
          </>
        )}
        {deviceInformation?.metadata !== undefined && (
          <>
            <Title size="lg" headingLevel="h5">
              MetaData
            </Title>
            {/* {deviceInformation.metadata} */}
            --
          </>
        )}
        <br />
        <br />
      </>
    );
  };

  const getConnectionType = () => {
    const { connectionType } = device || {};
    return (
      <>
        <Text className={css(style.font_size_20)}>Connection Type</Text>
        <br />
        {connectionType === "directly"
          ? "Connected directly"
          : "Connected via gateways"}
        <br />
        <br />
      </>
    );
  };

  const getGateways = () => {
    const { gateways } = device || {};
    return (
      <>
        <Text className={css(style.font_size_20)}>Gateways</Text>
        {gateways && gateways.gateways && gateways.gateways.length
          ? gateways.gateways.map(gateway => (
              <span key="gateway">
                <br />
                {gateway}
              </span>
            ))
          : "--"}
        <br />
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
            id={"review-device-credntails-list"}
            credentials={credentials}
            enableActions={false}
          />
        )}
      </>
    );
  };
  return (
    <>
      <Grid style={{ padding: 10 }}>
        <GridItem span={8}>
          {title && title.trim() != "" && (
            <>
              <Title size="2xl" headingLevel="h2">
                {title}
              </Title>
              <br />
            </>
          )}
          <br />
          {device && device.deviceInformation && getDeviceInformation()}
          {device && device.connectionType && getConnectionType()}
          {device &&
            device.gateways &&
            device.gateways.gateways &&
            getGateways()}
          {device &&
            device.credentials &&
            device.credentials.length > 0 &&
            getCredentials()}
        </GridItem>
      </Grid>
    </>
  );
};

export { ReviewDevice };
