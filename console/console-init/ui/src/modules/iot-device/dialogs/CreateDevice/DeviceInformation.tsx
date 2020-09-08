/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import {
  Form,
  FormGroup,
  TextInput,
  Title,
  Divider,
  Switch,
  Flex,
  Grid,
  GridItem,
  FlexItem
} from "@patternfly/react-core";
import { CreateMetadata, IMetadataProps } from "modules/iot-device/components";
import { StyleSheet, css } from "aphrodite";
import { MetadataHeader } from "modules/iot-device/components/CreateMetadata/MetadataHeader";

export interface IDeviceInfo {
  deviceId?: string;
  returnDeviceId?: (value: string) => void;
  deviceStatus?: boolean;
  returnDeviceStatus?: (value: boolean) => void;
  metadataList?: IMetadataProps[];
  returnMetadataList?: (value: IMetadataProps[]) => void;
}

export const DeviceInformation: React.FunctionComponent<IDeviceInfo> = ({
  deviceId,
  returnDeviceId,
  deviceStatus,
  returnDeviceStatus,
  metadataList,
  returnMetadataList
}) => {
  const [deviceIdInput, setDeviceIdInput] = useState<string>(deviceId || "");
  const [isDeviceEnabled, setIsDeviceEnabled] = useState<boolean>(
    deviceStatus || true
  );
  const handleDeviceIdChange = (val: string) => {
    setDeviceIdInput(val);
  };
  const handleStatusChange = (val: boolean) => {
    setIsDeviceEnabled(val);
  };

  const styles = StyleSheet.create({
    lighter_text: {
      fontWeight: "lighter"
    }
  });

  useEffect(() => {
    returnDeviceId && returnDeviceId(deviceIdInput);
  }, [deviceIdInput]);

  useEffect(() => {
    returnDeviceStatus && returnDeviceStatus(isDeviceEnabled);
  }, [isDeviceEnabled]);

  return (
    <Grid>
      <GridItem span={8}>
        <Form>
          <Title id="device-info-title" headingLevel="h3" size="xl">
            Enter your device information{" "}
            <small className={css(styles.lighter_text)}>(Optional)</small>
          </Title>
          <FormGroup
            id="cd-device-info-device-id"
            label="Device ID"
            fieldId="device-info-id-input"
          >
            <TextInput
              isRequired
              type="text"
              id="device-info-id-input"
              name="device-id"
              aria-describedby="device-id-helper"
              value={deviceIdInput}
              onChange={handleDeviceIdChange}
            />
            <small>
              A device ID will be automatically generated if it's not specified
              here.
            </small>
          </FormGroup>
          <FormGroup
            id="cd-device-info-form-switch"
            label="Status"
            fieldId="device-info-switchtoggle"
          >
            <Flex>
              <FlexItem>
                {" "}
                <small> Enable or disable this device. </small>
              </FlexItem>
              <FlexItem align={{ default: "alignRight" }}>
                <Switch
                  id="device-info-status-switch"
                  label="Enabled"
                  labelOff="Disabled"
                  isChecked={isDeviceEnabled}
                  onChange={handleStatusChange}
                />
              </FlexItem>
            </Flex>
          </FormGroup>
          <br />
          <Divider />
          Metadata
          <MetadataHeader sectionName="Default properties parameter" />
          <CreateMetadata
            metadataList={metadataList}
            returnMetadataList={returnMetadataList}
          />
          <MetadataHeader sectionName="Extensions parameter" />
          <CreateMetadata
            metadataList={metadataList}
            returnMetadataList={returnMetadataList}
          />
        </Form>
      </GridItem>
    </Grid>
  );
};
