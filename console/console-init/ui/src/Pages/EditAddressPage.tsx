import React, { useState } from "react";
import {
  Form,
  FormGroup,
  TextInput,
  FormSelect,
  FormSelectOption
} from "@patternfly/react-core";
import { IAddress } from "../Components/AddressList";

interface IEditAddressProps {
  address: IAddress;
  onChange: (address: IAddress) => void;
}

export const EditAddress: React.FunctionComponent<IEditAddressProps> = ({
  address
}) => {
  //TODO: Call GraphQL to fetch values of Type and Plan
  const options = [
    { value: "Queue", label: "Queue", disabled: false },
    { value: "miss", label: "Miss", disabled: false },
    { value: "mrs", label: "Mrs", disabled: false },
    { value: "ms", label: "Ms", disabled: false },
    { value: "dr", label: "Dr", disabled: false },
    { value: "other", label: "Other", disabled: false }
  ];
  const [plan, setPlan] = useState<string>("mrs");
  const [type, setType] = useState<string>("mrs");
  const [name, setName] = useState<string>("");
  return (
    <Form>
      <FormGroup label="Name" fieldId="simple-form-name">
        <TextInput
          type="text"
          id="simple-form-name"
          name="simple-form-name"
          aria-describedby="simple-form-name-helper"
          value={name}
          onChange={value => setName(value)}
        />
      </FormGroup>
      <FormGroup label="Type" fieldId="simple-form-name">
        <FormSelect
          value={type}
          onChange={value => setType(value)}
          aria-label="FormSelect Input"
        >
          {options.map((option, index) => (
            <FormSelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              label={option.label}
            />
          ))}
        </FormSelect>
      </FormGroup>
      <FormGroup label="Plan" fieldId="simple-form-name">
        <FormSelect
          value={plan}
          onChange={value => setPlan(value)}
          aria-label="FormSelect Input"
        >
          {options.map((option, index) => (
            <FormSelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              label={option.label}
            />
          ))}
        </FormSelect>
      </FormGroup>
    </Form>
  );
};
