/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
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
  SelectOptionObject,
  FlexItem
} from "@patternfly/react-core";
import { CreateMetadata } from "modules/iot-device/components";
import { SwitchWithToggle } from "components/SwitchWithToggle/SwitchWithToggle";
import { StyleSheet, css } from "aphrodite";

export interface IDeviceInfo {
  onPropertySelect?: (e: any, selection: SelectOptionObject) => void;
  onChangePropertyInput?: (value: string) => Promise<any>;
  onPropertyClear?: () => void;
  propertySelected?: string;
  propertyInput?: string;
  setPropertyInput?: (value: string) => void;
}

export const DeviceInformation: React.FunctionComponent<IDeviceInfo> = ({
  onChangePropertyInput
}) => {
  const [deviceIdInput, setDeviceIdInput] = useState("");
  const [isChecked, setIsChecked] = useState(true);
  const handleTextInputChange1 = () => {};
  const handleChange = () => {
    setIsChecked(!isChecked);
  };

  const styles = StyleSheet.create({
    lighter_text: {
      fontWeight: "lighter"
    }
  });
  return (
    <Grid>
      <GridItem span={8}>
        <Form>
          <Title id="cd-device-info-title" headingLevel="h3" size="xl">
            Enter your device information{" "}
            <small className={css(styles.lighter_text)}>(Optional)</small>
          </Title>
          <FormGroup
            id="cd-device-info-device-id"
            label="Device ID"
            isRequired
            fieldId="device-info-id-input"
          >
            <TextInput
              isRequired
              type="text"
              id="device-info-id-input"
              name="device-id"
              aria-describedby="device-id-helper"
              value={deviceIdInput}
              onChange={handleTextInputChange1}
            />
            <small>
              A device ID will be automatically generated if it's not specified
              here.
            </small>
          </FormGroup>
          <FormGroup
            id="cd-device-info-form-switch"
            label={<p className={css(styles.lighter_text)}>Status</p>}
            fieldId="device-info-switchtoggle"
          >
            <Flex>
              <FlexItem>
                {" "}
                <small> Enable or disable this device. </small>
              </FlexItem>
              <FlexItem align={{ default: "alignRight" }}>
                <Switch
                  id="cd-device-info-switch"
                  label="Enabled"
                  labelOff="Disabled"
                  isChecked={isChecked}
                  onChange={handleChange}
                />
              </FlexItem>
            </Flex>
          </FormGroup>
          <br />
          <Divider />
          Metadata
          <CreateMetadata />
        </Form>
      </GridItem>
    </Grid>
  );
};
