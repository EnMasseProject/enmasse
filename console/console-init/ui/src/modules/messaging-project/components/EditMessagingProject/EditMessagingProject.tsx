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
      id="edit-messaging-project-modal"
      title="Edit"
      isOpen={true}
      onClose={onCloseDialog}
      actions={[
        <Button
          key="confirm"
          id="edit-messaging-project-confirm-button"
          aria-label="confirm"
          variant="primary"
          onClick={onConfirmDialog}
        >
          Confirm
        </Button>,
        <Button
          key="cancel"
          id="edit-messaging-project-cancel-button"
          aria-label="cancel"
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
            id="edit-messaging-project-namespace-select"
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
        <FormGroup
          label="Name"
          fieldId="edit-messaging-project-name-input"
          isRequired={true}
        >
          <TextInput
            type="text"
            id="edit-messaging-project-name-input"
            isDisabled
            value={addressSpace.name}
          />
        </FormGroup>
        <FormGroup
          label="Type"
          fieldId="edit-messaging-project-type-radio"
          isInline
          isRequired={true}
        >
          <Radio
            name="radio-1"
            isDisabled
            label="Standard"
            id="edit-messaging-project-standard-type-radio"
            value="standard"
            isChecked={addressSpace.type === "standard"}
          />
          <Radio
            name="radio-2"
            isDisabled
            label="Brokered"
            id="edit-messaging-project-brokered-type-radio"
            value="brokered"
            isChecked={addressSpace.type === "brokered"}
          />
        </FormGroup>
        <FormGroup
          label="Address space plan"
          fieldId="edit-messaging-project-plan-select"
          isRequired={true}
        >
          <FormSelect
            id="edit-messaging-project-plan-select"
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
          fieldId="edit-messaging-project-auth-service-select"
          isRequired={true}
        >
          <FormSelect
            id="edit-messaging-project-auth-service-select"
            value={addressSpace.authenticationService}
            onChange={val => onAuthServiceChange(val)}
            aria-label="Authentication service"
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
