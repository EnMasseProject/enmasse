import React, { useState } from "react";
import {
  Form,
  FormGroup,
  TextInput,
  Title,
  Switch,
  Divider,
  Grid,
  GridItem
} from "@patternfly/react-core";
import { MetaData } from "./MetaData";
import { IDeviceInfo } from ".";

export const DeviceInformation: React.FunctionComponent<IDeviceInfo> = ({
  onPropertySelect,
  onChangePropertyInput,
  onPropertyClear,
  propertySelected,
  propertyInput,
  setPropertyInput
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
          <Title headingLevel="h3" size="2xl">
            Enter your device information
          </Title>
          <FormGroup label="Device ID" isRequired fieldId="device-id">
            <TextInput
              isRequired
              type="text"
              id="device-id"
              name="device-id"
              aria-describedby="device-id-helper"
              value={deviceIdInput}
              onChange={handleTextInputChange1}
            />
          </FormGroup>
          <FormGroup label="Status" fieldId="simple-form-status">
            <br />
            Enable or disable this device{" "}
            <Switch
              id="simple-switch"
              label="Enabled"
              labelOff="Disabled"
              isChecked={isChecked}
              onChange={handleChange}
            />
          </FormGroup>
          <Divider />
          Metadata
          <MetaData
            onPropertyClear={onPropertyClear}
            onPropertySelect={onPropertySelect}
            onChangePropertyInput={onChangePropertyInput}
            propertySelected={propertySelected}
            propertyInput={propertyInput}
            setPropertyInput={setPropertyInput}
          />
        </Form>
      </GridItem>
    </Grid>
  );
};
