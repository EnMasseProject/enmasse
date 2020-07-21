/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Link } from "react-router-dom";
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
  Text,
  TextVariants,
  CardTitle
} from "@patternfly/react-core";
import { StyleSheet, css } from "aphrodite";
import {
  CredentialsView,
  ICredentialsViewProps
} from "modules/iot-device-detail/components";
import { SwitchWithToggle, JsonEditor, MetadataListTable } from "components";
import { ErrorStateAlert, IErrorStateAlertProps } from "./ErrorStateAlert";

const styles = StyleSheet.create({
  card_body: {
    paddingLeft: 0,
    paddingRight: 0,
    paddingBottom: 0,
    minHeight: 336
  }
});

export interface IDeviceInfoProps
  extends Pick<
      ICredentialsViewProps,
      "credentials" | "onChangeStatus" | "onConfirmPassword"
    >,
    IErrorStateAlertProps {
  id: string;
  deviceList?: any;
  metadataList?: any;
}

export const DeviceInfo: React.FC<IDeviceInfoProps> = ({
  id,
  deviceList,
  metadataList: metadetaJson,
  credentials,
  onChangeStatus,
  onConfirmPassword,
  errorState,
  deleteGateways,
  deleteCredentials
}) => {
  const [isHidden, setIsHidden] = useState<boolean>(false);
  const { projectname, namespace } = useParams();

  const jsonViewData = {
    via: deviceList,
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
              <Card>
                <CardTitle>
                  <Title
                    id="device-info-gateways-title"
                    headingLevel="h1"
                    size="2xl"
                  >
                    Via gateways
                  </Title>
                </CardTitle>
                <CardBody>
                  <Grid>
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
                    {!(deviceList?.length > 0) && (
                      <Text
                        component={TextVariants.p}
                        id="device-info-no-gateways-text"
                      >
                        There are no gateways for this device. This device is
                        connected to the cloud directly.
                      </Text>
                    )}
                  </Grid>
                </CardBody>
              </Card>
              <br />
              <CredentialsView
                id={"credentials-view"}
                credentials={credentials}
                onChangeStatus={onChangeStatus}
                onConfirmPassword={onConfirmPassword}
              />
            </GridItem>
            <GridItem span={7}>
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
