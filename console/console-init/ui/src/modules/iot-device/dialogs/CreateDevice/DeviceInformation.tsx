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
  Grid,
  GridItem,
  SelectOptionObject
} from "@patternfly/react-core";
import { CreateMetadata } from "modules/iot-device/components";
import { SwitchWithToggle } from "components/SwitchWithToggle/SwitchWithToggle";

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
  return (
    <Grid>
      <GridItem span={8}>
        <Form>
          <Title id="cd-device-info-title" headingLevel="h3" size="2xl">
            Enter your device information
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
          </FormGroup>
          <FormGroup
            id="cd-device-info-form-switch"
            label="Status"
            fieldId="device-info-switchtoggle"
          >
            <br />
            Enable or disable this device{" "}
            <SwitchWithToggle
              id="device-info-switchtoggle"
              label="Enabled"
              labelOff="Disabled"
              isChecked={isChecked}
              onChange={handleChange}
            />
          </FormGroup>
          <Divider />
          Metadata
          <CreateMetadata />
        </Form>
      </GridItem>
    </Grid>
  );
};
