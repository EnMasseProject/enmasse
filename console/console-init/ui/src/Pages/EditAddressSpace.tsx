import React from "react";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_ADDRESS_PLANS } from "src/Queries/Queries";
import { Form, TextContent, Text, TextVariants, FormGroup, FormSelect, FormSelectOption, TextInput, Radio } from "@patternfly/react-core";
import { IAddressSpace } from "src/Components/AddressSpaceList/AddressSpaceList";
import { IAddressSpacePlans } from "src/Components/AddressSpace/CreateAddressSpaceConfiguration";

interface IEditAddressSpaceProps {
  addressSpace: IAddressSpace;
  onPlanChange: (type: string) => void;
}

export const EditAddressSpace: React.FunctionComponent<IEditAddressSpaceProps> = ({
  addressSpace, onPlanChange
}) => {

  // const { addressPlans } = useQuery<IAddressSpacePlans>(RETURN_ADDRESS_PLANS)
  //   .data || {
  //   addressPlans: []
  // };
  // TODO populate using data from backend

  let planOptions: any[] = [
    {value: "small queue", label: "Small queue", isDisabled: false},
    {value: "medium queue", label: "Medium queue", isDisabled: false},
    {value: "large queue", label: "Large queue", isDisabled: false},
  ];

  return (
    <Form>
      <TextContent>
        <Text component={TextVariants.h2}>Choose a new plan.</Text>
      </TextContent>
      <FormGroup label="Namespace" fieldId="name-space" isRequired={true}>
        <FormSelect
          id="edit-addr-plan"
          isDisabled
          value={addressSpace.nameSpace}
          aria-label="FormSelect Input">
          <FormSelectOption
            value={addressSpace.nameSpace}
            label={addressSpace.nameSpace}
          />
        </FormSelect>
      </FormGroup>
      <FormGroup label="Name" fieldId="address-space" isRequired={true}>
        <TextInput
          type="text"
          id="as-name"
          isDisabled
          value={addressSpace.name}
        />
      </FormGroup>
      <FormGroup label="Type" fieldId="address-space-type" isInline isRequired={true}>
        <Radio
          name="radio-1"
          isDisabled
          label="Standard"
          id="radio-standard"
          value="standard"
          isChecked={addressSpace.type === "standard"}
        />
        <Radio
          name="radio-2"
          isDisabled
          label="Brokered"
          id="radio-brokered"
          value="brokered"
          isChecked={addressSpace.type === "brokered"}
        />
      </FormGroup>
      <FormGroup label="Address space plan" fieldId="simple-form-name" isRequired={true}>
        <FormSelect
          id="edit-addr-plan"
          value={addressSpace.displayName}
          onChange={(val) => onPlanChange(val)}
          aria-label="FormSelect Input">
          {planOptions.map((option, index) => (
            <FormSelectOption
              isDisabled={option.disabled}
              key={index}
              value={option.value}
              label={option.label}
            />
          ))}
        </FormSelect>
      </FormGroup>
      <FormGroup label="Authentication Service" fieldId="simple-form-name" isRequired={true}>
        <FormSelect
          id="edit-addr-auth"
          value={"sample"}
          isDisabled
          aria-label="FormSelect Input">
          <FormSelectOption
            value={"sample"}
            label={"Sample"}
          />
        </FormSelect>
      </FormGroup>
    </Form>
  );
}