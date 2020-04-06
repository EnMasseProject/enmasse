import React, { useState } from "react";
import {
  Form,
  FormGroup,
  TextInput,
  Checkbox,
  Text,
  TextVariants,
  Title,
  Switch,
  Divider
} from "@patternfly/react-core";
import { MetaData } from "./MetaData";

export const DeviceInformation: React.FunctionComponent<{}> = () => {
  const [value1, setValue1] = useState("");
  const [value2, setValue2] = useState("");
  const [value3, setValue3] = useState("");
  const [isChecked, setIsChecked] = useState(true);
  const handleTextInputChange1 = () => {};
  const handleTextInputChange2 = () => {};
  const handleTextInputChange3 = () => {};
  const handleChange = () => {
    setIsChecked(!isChecked);
  };
  return (
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
          value={value1}
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
      <MetaData />
    </Form>
  );
};
