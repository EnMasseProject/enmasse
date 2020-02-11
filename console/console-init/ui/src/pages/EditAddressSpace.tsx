/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useQuery } from "@apollo/react-hooks";
import {
  RETURN_ADDRESS_SPACE_PLANS,
  RETURN_FILTERED_AUTHENTICATION_SERVICES
} from "queries";
import {
  Form,
  TextContent,
  Text,
  TextVariants,
  FormGroup,
  FormSelect,
  FormSelectOption,
  TextInput,
  Radio
} from "@patternfly/react-core";
import { IAddressSpace } from "components/AddressSpaceList/AddressSpaceList";
import {
  IAddressSpacePlans,
  IAddressSpaceAuthServiceResponse
} from "pages/CreateAddressSpace/CreateAddressSpaceConfiguration";
import { Loading } from "use-patternfly";

interface IEditAddressSpaceProps {
  addressSpace: IAddressSpace;
  onPlanChange: (type: string) => void;
  onAuthServiceChanged: (type: string) => void;
}

export const EditAddressSpace: React.FunctionComponent<IEditAddressSpaceProps> = ({
  addressSpace,
  onPlanChange,
  onAuthServiceChanged
}) => {
  const { loading, error, data } = useQuery<IAddressSpacePlans>(
    RETURN_ADDRESS_SPACE_PLANS
  );

  const authServices = useQuery<IAddressSpaceAuthServiceResponse>(
    RETURN_FILTERED_AUTHENTICATION_SERVICES,
    {
      variables: {
        t: addressSpace.type
      }
    }
  ).data || { addressSpaceSchema_v2: [] };

  if (loading) return <Loading />;
  if (error) return <Loading />;

  const { addressSpacePlans } = data || {
    addressSpacePlans: []
  };

  let planOptions: any[] = [];

  let authServiceOptions: any[] = [];

  if (addressSpace.type) {
    planOptions =
      addressSpacePlans
        .map(plan => {
          if (plan.spec.addressSpaceType === addressSpace.type) {
            return {
              value: plan.objectMeta.name,
              label: plan.objectMeta.name
            };
          }
        })
        .filter(plan => plan !== undefined) || [];
  }

  if (authServices.addressSpaceSchema_v2[0])
    authServiceOptions = authServices.addressSpaceSchema_v2[0].Spec.AuthenticationServices.map(
      authService => {
        return {
          value: authService,
          label: authService
        };
      }
    );

  return (
    <Form>
      <TextContent>
        <Text component={TextVariants.h2}>Choose a new plan.</Text>
      </TextContent>
      <FormGroup label="Namespace" fieldId="name-space" isRequired={true}>
        <FormSelect
          id="edit-namespace"
          isDisabled
          value={addressSpace.nameSpace}
          aria-label="FormSelect Input"
        >
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
      <FormGroup
        label="Type"
        fieldId="address-space-type"
        isInline
        isRequired={true}
      >
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
      <FormGroup
        label="Address space plan"
        fieldId="simple-form-name"
        isRequired={true}
      >
        <FormSelect
          id="edit-addr-plan"
          value={addressSpace.planValue}
          onChange={val => onPlanChange(val)}
          aria-label="FormSelect Input"
        >
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
      <FormGroup
        label="Authentication Service"
        fieldId="simple-form-name"
        isRequired={true}
      >
        <FormSelect
          id="edit-addr-auth"
          value={addressSpace.authenticationService}
          onChange={val => onAuthServiceChanged(val)}
          aria-label="FormSelect Input"
        >
          {authServiceOptions.map((option, index) => (
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
