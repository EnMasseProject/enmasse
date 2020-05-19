/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Page,
  PageSection,
  Grid,
  GridItem,
  Split,
  SplitItem
} from "@patternfly/react-core";
import { StyleSheet } from "@patternfly/react-styles";
import {
  DeviceInfoGateways,
  CredentialsView,
  ICredentialsViewProps,
  DeviceInfoMetadata
} from "modules/device-detail/components";
import { SwitchWithToggle, JsonEditor } from "components";

const styles = StyleSheet.create({
  gateways_align: {
    marginRight: 20
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
  metadataList,
  credentials
}) => {
  const jsonViewData = {
    credentials
  };
  const [isHidden, setIsHidden] = useState<boolean>(false);

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
          <Grid>
            <GridItem span={5} className={styles.gateways_align}>
              <DeviceInfoGateways deviceList={deviceList} />
              <br />
              <CredentialsView
                id={"credentials-view"}
                credentials={credentials}
              />
            </GridItem>
            <GridItem span={7}>
              <DeviceInfoMetadata
                dataList={metadataList}
                id={"divice-info-metadata"}
              />
            </GridItem>
          </Grid>
        )}
      </PageSection>
    </Page>
  );
};
