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
import { ViewMetaData } from "modules/iot-device";
interface IReviewDeviceProps {
  device?: IDeviceProp;
  title?: string;
}

const style = StyleSheet.create({
  font_size_20: {
    fontSize: 20
  },
  split_right_margin: {
    marginRight: "1rem"
  }
});

const ReviewDevice: React.FunctionComponent<IReviewDeviceProps> = ({
  device,
  title
}) => {
  const getDeviceInformation = () => {
    const { status, defaults, ext } = device?.registration || {};
    return (
      <>
        <Text className={css(style.font_size_20)}>Device Information</Text>
        <br />
        {device?.id && (
          <>
            <Title size="lg" headingLevel="h5">
              Devicedevice. ID
            </Title>
            {device.id}
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
        <>
          <Title size="lg" headingLevel="h5">
            MetaData
          </Title>
          <br />
          <ViewMetaData defaults={defaults} ext={ext} />
          <br />
          <br />
        </>
      </>
    );
  };

  const getConnectionType = () => {
    const { gateways, credentials } = device || {};

    let connectionType: string = "";
    const viaGateway = gateways
      ? (gateways.gateways && gateways.gateways.length > 0) ||
        (gateways.gatewayGroups && gateways.gatewayGroups.length > 0)
      : false;
    if (credentials && credentials.length > 0 && !viaGateway) {
      connectionType = "Connected directly";
    } else if (viaGateway && !credentials?.length) {
      connectionType = "Connected via gateways";
    } else {
      connectionType = "N/A";
    }

    return (
      <>
        <Text className={css(style.font_size_20)}>Connection Type</Text>
        <br />
        {connectionType}
        <br />
        <br />
      </>
    );
  };
  const renderSplitItems = (items?: string[]) => {
    return (
      <>
        <Split>
          {items && items?.length > 0
            ? items.map(item => (
                <SplitItem key={item} className={css(style.split_right_margin)}>
                  {item}
                </SplitItem>
              ))
            : "--"}
        </Split>
      </>
    );
  };

  const getGateways = () => {
    const { gateways } = device?.gateways || {};
    return (
      <>
        <Text className={css(style.font_size_20)}>Gateways</Text>
        <br />
        <Split>{renderSplitItems(gateways)}</Split>
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
        {renderSplitItems(gatewayGroups)}
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
        {renderSplitItems(memberOf)}
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
          <>
            <CredentialsView
              id={"review-device-crdentials-list"}
              credentials={credentials}
              enableActions={false}
            />
            <br />
          </>
        )}
      </>
    );
  };

  const { registration, connectionType, credentials, gateways, memberOf } =
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
          {registration && getDeviceInformation()}
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
