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

export const MetaData: React.FunctionComponent<{}> = () => {
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
      <FormGroup
        label="Device Message info Parameter"
        isRequired
        fieldId="device-id"
      >
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
      <FormGroup
        label="Device basic info parameter"
        fieldId="simple-form-status"
      >
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
    </Form>
  );
};
