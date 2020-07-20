/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Form,
  TextContent,
  Text,
  TextVariants,
  FormGroup,
  FormSelect,
  FormSelectOption,
  TextInput,
  Radio,
  Button,
  Modal
} from "@patternfly/react-core";
import { IAddressSpace } from "modules/address-space";

export interface IEditAddressSpaceProps {
  onCloseDialog: () => void;
  onConfirmDialog: () => void;
  onPlanChange: (plan: string) => void;
  onAuthServiceChange: (authservice: string) => void;
  authServiceOptions: any[];
  planOptions: any[];
  addressSpace: IAddressSpace;
}

export const EditAddressSpace: React.FC<IEditAddressSpaceProps> = ({
  onCloseDialog,
  onConfirmDialog,
  onPlanChange,
  onAuthServiceChange,
  authServiceOptions,
  planOptions,
  addressSpace
}) => {
  return (
    <Modal
      variant="large"
      id="addr-space-edit-modal"
      title="Edit"
      isOpen={true}
      onClose={onCloseDialog}
      actions={[
        <Button
          key="confirm"
          id="addr-space-confirm-edit-button"
          aria-label="confirm button"
          variant="primary"
          onClick={onConfirmDialog}
        >
          Confirm
        </Button>,
        <Button
          key="cancel"
          id="addr-space-cancel-button"
          aria-label="cancel button"
          variant="link"
          onClick={onCloseDialog}
        >
          Cancel
        </Button>
      ]}
    >
      <Form>
        <TextContent>
          <Text component={TextVariants.h2}>Choose a new plan.</Text>
        </TextContent>
        <FormGroup
          label="Namespace"
          fieldId="addr-space-edit-namespace-formselect"
          isRequired={true}
        >
          <FormSelect
            id="addr-space-edit-namespace-formselect"
            isDisabled
            value={addressSpace.nameSpace}
            aria-label="FormSelect Input"
          >
            <FormSelectOption
              id="addr-space-edit-namespace-formselectoption"
              value={addressSpace.nameSpace}
              label={addressSpace.nameSpace}
            />
          </FormSelect>
        </FormGroup>
        <FormGroup
          label="Name"
          fieldId="address-space-name-input"
          isRequired={true}
        >
          <TextInput
            type="text"
            id="address-space-name-input"
            isDisabled
            value={addressSpace.name}
          />
        </FormGroup>
        <FormGroup
          label="Type"
          fieldId="address-space-type-radio"
          isInline
          isRequired={true}
        >
          <Radio
            name="radio-1"
            isDisabled
            label="Standard"
            id="address-space-standard-radio"
            value="standard"
            isChecked={addressSpace.type === "standard"}
          />
          <Radio
            name="radio-2"
            isDisabled
            label="Brokered"
            id="address-space-brokered-radio"
            value="brokered"
            isChecked={addressSpace.type === "brokered"}
          />
        </FormGroup>
        <FormGroup
          label="Address space plan"
          fieldId="addr-space-plan-formselect"
          isRequired={true}
        >
          <FormSelect
            id="addr-space-plan-formselect"
            value={addressSpace.planValue}
            onChange={val => onPlanChange(val)}
            aria-label="FormSelect Input"
          >
            {planOptions.map((option, index) => (
              <FormSelectOption
                id="addr-space-plan-formselectoption"
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
          fieldId="addr-space-authentication-formselect"
          isRequired={true}
        >
          <FormSelect
            id="addr-space-authentication-formselect"
            value={addressSpace.authenticationService}
            onChange={val => onAuthServiceChange(val)}
            aria-label="FormSelect Input"
          >
            {authServiceOptions.map((option, index) => (
              <FormSelectOption
                id="addr-space-authentication-formselectoption"
                isDisabled={option.disabled}
                key={index}
                value={option.value}
                label={option.label}
              />
            ))}
          </FormSelect>
        </FormGroup>
      </Form>
    </Modal>
  );
};
