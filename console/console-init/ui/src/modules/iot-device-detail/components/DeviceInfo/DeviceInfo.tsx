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
  CardHeader,
  Title,
  Text,
  TextVariants
} from "@patternfly/react-core";
import { StyleSheet, css } from "@patternfly/react-styles";
import {
  CredentialsView,
  ICredentialsViewProps
} from "modules/iot-device-detail/components";
import { SwitchWithToggle, JsonEditor, MetadataListTable } from "components";

const styles = StyleSheet.create({
  card_body: {
    paddingLeft: 0,
    paddingRight: 0,
    paddingBottom: 0
  }
});

export interface IDeviceInfoProps
  extends Pick<ICredentialsViewProps, "credentials"> {
  id: string;
  deviceList?: any;
  metadataList?: any;
}

export const DeviceInfo: React.FC<IDeviceInfoProps> = ({
  id,
  deviceList,
  metadataList: metadetaJson,
  credentials
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
      <PageSection>
        <Split>
          <SplitItem isFilled />
          <SplitItem>
            <SwitchWithToggle
              id={"divice-info-view-json"}
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
            maxLines={45}
          />
        ) : (
          <Grid gutter="sm">
            <GridItem span={5}>
              <Card>
                <CardHeader>
                  <Title id="di-header-title" headingLevel="h1" size="2xl">
                    Via gateways
                  </Title>
                </CardHeader>
                <CardBody>
                  <Grid>
                    {deviceList &&
                      deviceList.map((deviceId: string) => {
                        return (
                          <GridItem span={2} key={deviceId}>
                            <Link
                              to={`/iot-projects/${namespace}/${projectname}/devices/${deviceId}/device-info`}
                            >
                              {deviceId}
                            </Link>
                          </GridItem>
                        );
                      })}
                    {!(deviceList && deviceList.length > 0) && (
                      <Text component={TextVariants.p}>
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
              />
            </GridItem>
            <GridItem span={7}>
              <Card id={id}>
                <CardHeader>
                  <Title headingLevel="h1" size="2xl">
                    Device metadata
                  </Title>
                </CardHeader>
                <CardBody className={css(styles.card_body)}>
                  <MetadataListTable
                    dataList={prepareMetadataList()}
                    id={"divice-info-metadata-table"}
                    aria-label={"device info metadata"}
                    aria-labelledby-header={"device info metadata header"}
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
