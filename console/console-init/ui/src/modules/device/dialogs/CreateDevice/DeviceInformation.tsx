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
  GridItem
} from "@patternfly/react-core";
import { MetaData } from "./MetaData";
import { IDeviceInfo } from ".";
import { SwitchWithToggle } from "components/SwitchWithToggle/SwitchWithToggle";

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
            fieldId="cd-device-info-device-id"
          >
            <TextInput
              isRequired
              type="text"
              id="cd-device-info-text-device-id"
              name="device-id"
              aria-describedby="device-id-helper"
              value={deviceIdInput}
              onChange={handleTextInputChange1}
            />
          </FormGroup>
          <FormGroup
            id="cd-device-info-form-switch"
            label="Status"
            fieldId="cd-device-info-form-switch"
          >
            <br />
            Enable or disable this device{" "}
            <SwitchWithToggle
              id="cd-device-info-switch"
              label="Enabled"
              labelOff="Disabled"
              isChecked={isChecked}
              onChange={handleChange}
            />
          </FormGroup>
          <Divider />
          Metadata
          <MetaData onChangePropertyInput={onChangePropertyInput} />
        </Form>
      </GridItem>
    </Grid>
  );
};
