import React, { useState } from "react";
import {
  Form,
  FormGroup,
  TextInput,
  FormSelect,
  FormSelectOption
} from "@patternfly/react-core";
import { IAddress } from "../Components/AddressSpace/AddressList";

interface IEditAddressProps {
  address: IAddress;
  onChange: (address: IAddress) => void;
}

export const EditAddress: React.FunctionComponent<IEditAddressProps> = ({
  address
}) => {
  //TODO: Call GraphQL to fetch values of Type and Plan
  const optionsType = [
    { value: "Queue", label: "Queue", disabled: false },
    { value: "Topic", label: "Topic", disabled: false },
    { value: "Subscription", label: "Subscription", disabled: false },
    { value: "Multicast", label: "Multicast", disabled: false },
    { value: "Anycast", label: "Anycast", disabled: false },

  ];

  const optionsPlan = [
    { value: "Small", label: "Small", disabled: false },
    { value: "Large", label: "Large", disabled: false },
    { value: "Medium", label: "Medium", disabled: false }

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
          {optionsType.map((option, index) => (
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
          {optionsPlan.map((option, index) => (
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
