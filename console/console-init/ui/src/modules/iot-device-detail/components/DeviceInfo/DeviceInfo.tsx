/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useParams } from "react-router";
import {
  Page,
  PageSection,
  Grid,
  GridItem,
  Split,
  SplitItem,
  Card,
  CardBody,
  Title,
  CardTitle
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import { ConnectionGateway } from "modules/iot-device-detail/components/ConnectionGateway";
import { GatewayMembership } from "modules/iot-device-detail/components/GatewayMembership";
import {
  CredentialsView,
  ICredentialsViewProps
} from "modules/iot-device-detail/components";
import { SwitchWithToggle, JsonEditor, MetadataListTable } from "components";
import { ErrorStateAlert, IErrorStateAlertProps } from "./ErrorStateAlert";

const styles = StyleSheet.create({
  card_body: {
    paddingLeft: "0rem",
    paddingRight: "0rem",
    paddingBottom: "0rem",
    minHeight: "336rem"
  }
});

export interface IDeviceInfoProps
  extends Pick<
      ICredentialsViewProps,
      "credentials" | "onConfirmCredentialsStatus"
    >,
    IErrorStateAlertProps {
  id: string;
  deviceList?: string[];
  gatewayGroups?: string[];
  memberOf?: string[];
  metadataList?: any;
}

export const DeviceInfo: React.FC<IDeviceInfoProps> = ({
  id,
  deviceList,
  gatewayGroups,
  memberOf,
  metadataList: metadetaJson,
  credentials,
  errorState,
  deleteGateways,
  deleteCredentials,
  onConfirmCredentialsStatus
}) => {
  const [isHidden, setIsHidden] = useState<boolean>(false);
  const { projectname, namespace } = useParams();

  const jsonViewData = {
    via: deviceList,
    viaGroups: gatewayGroups,
    memberOf: memberOf,
    ...metadetaJson,
    credentials
  };

  const prepareMetadataList = () => {
    const metadataList = [];
    if (metadetaJson?.default) {
      metadataList.push({
        headers: ["Message info parameter", "Type", "Value"],
        data: metadetaJson?.default
      });
    }
    if (metadetaJson?.ext) {
      metadataList.push({
        headers: ["Basic info parameter", "Type", "Value"],
        data: metadetaJson?.ext
      });
    }
    return metadataList;
  };

  const onToggle = (isEnabled: boolean) => {
    setIsHidden(isEnabled);
  };

  return (
    <Page id={id}>
      <PageSection padding={{ default: "noPadding" }}>
        {errorState && (
          <>
            <ErrorStateAlert
              errorState={errorState}
              deleteGateways={deleteGateways}
              deleteCredentials={deleteCredentials}
            />
            <br />
          </>
        )}
        <br />
        <Split>
          <SplitItem isFilled />
          <SplitItem>
            <SwitchWithToggle
              id={"device-info-view-json-switch"}
              label={"View in JSON"}
              onChange={onToggle}
            />
          </SplitItem>
        </Split>
        <br />
        {isHidden ? (
          <JsonEditor
            readOnly={true}
            value={jsonViewData && JSON.stringify(jsonViewData, undefined, 2)}
          />
        ) : (
          <Grid hasGutter>
            <GridItem span={5}>
              {((deviceList && deviceList?.length > 0) ||
                (gatewayGroups && gatewayGroups?.length > 0)) && (
                <ConnectionGateway
                  deviceList={deviceList}
                  gatewayGroups={gatewayGroups}
                />
              )}

              <CredentialsView
                id={"deice-info-credentials-view"}
                credentials={credentials}
                onConfirmCredentialsStatus={onConfirmCredentialsStatus}
              />
            </GridItem>
            <GridItem span={7}>
              {memberOf && memberOf?.length > 0 && (
                <GatewayMembership memberOf={memberOf} />
              )}
              <Card id={id}>
                <CardTitle>
                  <Title
                    headingLevel="h1"
                    size="2xl"
                    id="device-info-metadata-title"
                  >
                    Device metadata
                  </Title>
                </CardTitle>
                <CardBody className={css(styles.card_body)}>
                  <MetadataListTable
                    dataList={prepareMetadataList()}
                    id={"device-info-metadata-table"}
                    aria-label={"device info metadata"}
                    aria-labelledby={"device info metadata header"}
                  />
                </CardBody>
              </Card>
            </GridItem>
          </Grid>
        )}
      </PageSection>
    </Page>
  );
};
