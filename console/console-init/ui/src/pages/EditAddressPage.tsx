/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Form,
  FormGroup,
  TextInput,
  FormSelect,
  FormSelectOption
} from "@patternfly/react-core";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_ADDRESS_PLANS } from "graphql-module/queries";
import { Loading } from "use-patternfly";

interface IEditAddressProps {
  name: string;
  type: string;
  plan: string;
  addressSpacePlan: string | null;
  onChange: (plan: string) => void;
}

interface IAddressPlans {
  addressPlans: Array<{
    metadata: {
      name: String;
    };
    spec: {
      addressType: string;
      displayName: string;
    };
  }>;
}

export const EditAddress: React.FunctionComponent<IEditAddressProps> = ({
  name,
  type,
  plan,
  addressSpacePlan,
  onChange
}) => {
  let { loading, error, data } = useQuery<IAddressPlans>(
    RETURN_ADDRESS_PLANS(addressSpacePlan || "", type)
  );

  if (loading) return <Loading />;
  if (error) return <Loading />;
  const { addressPlans } = data || {
    addressPlans: []
  };

  let optionsPlan: any[] = addressPlans
    .map(plan => {
      return {
        value: plan.metadata.name,
        label: plan.spec.displayName,
        disabled: false
      };
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
          id="edit-addr-name"
          name="simple-form-name"
          isDisabled
          aria-describedby="simple-form-name-helper"
          value={name}
        />
      </FormGroup>
      <FormGroup label="Type" fieldId="simple-form-name">
        <FormSelect
          isDisabled
          aria-label="FormSelect Input"
          id="edit-addr-type"
        >
          <FormSelectOption value={type} label={type} />
        </FormSelect>
      </FormGroup>
      <FormGroup label="Plan" fieldId="simple-form-name">
        <FormSelect
          id="edit-addr-plan"
          value={plan}
          onChange={value => onPlanChanged(value)}
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
