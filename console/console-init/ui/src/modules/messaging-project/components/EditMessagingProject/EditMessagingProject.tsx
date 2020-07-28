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
import { IAddressSpace } from "modules/messaging-project/components";

export interface IEditMessagingProjectProps {
  onCloseDialog: () => void;
  onConfirmDialog: () => void;
  onPlanChange: (plan: string) => void;
  onAuthServiceChange: (authservice: string) => void;
  authServiceOptions: any[];
  planOptions: any[];
  addressSpace: IAddressSpace;
}

export const EditMessagingProject: React.FC<IEditMessagingProjectProps> = ({
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
      id="as-list-edit-modal"
      title="Edit"
      isOpen={true}
      onClose={onCloseDialog}
      actions={[
        <Button
          key="confirm"
          id="as-list-edit-confirm"
          variant="primary"
          onClick={onConfirmDialog}
        >
          Confirm
        </Button>,
        <Button
          key="cancel"
          id="as-list-edit-cancel"
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
            onChange={val => onAuthServiceChange(val)}
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
    </Modal>
  );
};
