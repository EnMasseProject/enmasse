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
  const { deviceInformation, connectionType, credentials, gateways } =
    device || {};
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
          {deviceInformation && getDeviceInformation()}
          {connectionType && getConnectionType()}
          {gateways?.gateways && getGateways()}
          {credentials && credentials.length > 0 && getCredentials()}
        </GridItem>
      </Grid>
    </>
  );
};

export { ReviewDevice };
