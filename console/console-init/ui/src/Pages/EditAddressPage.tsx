import React from "react";
import {
  Form,
  FormGroup,
  TextInput,
  FormSelect,
  FormSelectOption
} from "@patternfly/react-core";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_ADDRESS_PLANS } from "src/Queries/Queries";
import { Loading } from "use-patternfly";

interface IEditAddressProps {
  name: string;
  type: string;
  plan: string;
  onChange: (plan: string) => void;
}

interface IAddressPlans {
  addressPlans: Array<{
    Spec: {
      AddressType: string;
      DisplayName: string;
    };
  }>;
}

export const EditAddress: React.FunctionComponent<IEditAddressProps> = ({
  name,
  type,
  plan,
  onChange
}) => {

  let { loading, error, data } = useQuery<IAddressPlans>(RETURN_ADDRESS_PLANS);

  if (loading) return <Loading />;
  if (error) return <Loading />;
  const { addressPlans } = data || {
    addressPlans: []
  };

  let optionsType: any[] = addressPlans.map(plan => {
    return {
      value: plan.Spec.AddressType,
      label:
        plan.Spec.AddressType.charAt(0).toUpperCase() +
        plan.Spec.AddressType.slice(1),
      disabled: false
    };
  });

  optionsType = optionsType.reduce((res, itm) => {
    let result = res.find(
      (item: any) => JSON.stringify(item) == JSON.stringify(itm)
    );
    if (!result) return res.concat(itm);
    return res;
  }, []);

  let optionsPlan: any[] = addressPlans
    .map(plan => {
      if (plan.Spec.AddressType === type) {
        return {
          value: plan.Spec.DisplayName,
          label: plan.Spec.DisplayName,
          disabled: false
        };
      }
    })
    .filter(plan => plan !== undefined);

  const onPlanChanged = (plan: string) => {
    onChange(plan);
  };

  return (
    <Form>
      <FormGroup label="Name" fieldId="simple-form-name">
        <TextInput
          type="text"
          id="simple-form-name"
          name="simple-form-name"
          isDisabled
          aria-describedby="simple-form-name-helper"
          value={name}
        />
      </FormGroup>
      <FormGroup label="Type" fieldId="simple-form-name">
        <FormSelect value={type} isDisabled aria-label="FormSelect Input">
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
          onChange={value => onPlanChanged(value)}
          aria-label="FormSelect Input">
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
