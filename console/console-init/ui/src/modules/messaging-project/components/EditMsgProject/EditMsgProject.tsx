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
import { IProject } from "modules/project";

export interface IEditMsgProjectProps {
  onCloseDialog: () => void;
  onConfirmDialog: () => void;
  onPlanChange: (plan: string) => void;
  onAuthServiceChange: (authservice: string) => void;
  authServiceOptions: any[];
  planOptions: any[];
  project: IProject;
}

export const EditMsgProject: React.FC<IEditMsgProjectProps> = ({
  onCloseDialog,
  onConfirmDialog,
  onPlanChange,
  onAuthServiceChange,
  authServiceOptions,
  planOptions,
  project
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
            value={project.namespace}
            aria-label="FormSelect Input"
          >
            <FormSelectOption
              id="edit-msg-project-namespace-option"
              value={project?.namespace}
              label={project?.namespace || ""}
            />
          </FormSelect>
        </FormGroup>
        <FormGroup label="Name" fieldId="address-space" isRequired={true}>
          <TextInput type="text" id="as-name" isDisabled value={project.name} />
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
            id="edit-msg-project-radio-standard"
            value="standard"
            isChecked={project.type === "standard"}
          />
          <Radio
            name="radio-2"
            isDisabled
            label="Brokered"
            id="edit-msg-project-radio-brokered"
            value="brokered"
            isChecked={project.type === "brokered"}
          />
        </FormGroup>
        <FormGroup
          label="Address space plan"
          fieldId="simple-form-name"
          isRequired={true}
        >
          <FormSelect
            id="edit-msg-project-edit-addr-plan"
            value={project.plan}
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
            id="edit-msg-project-auth-service"
            value={project.authService}
            onChange={val => onAuthServiceChange(val)}
            aria-label="FormSelect Input"
          >
            {authServiceOptions.map((option, index) => (
              <FormSelectOption
                id={`edit-msg-project-auth-service-option-${option.value}`}
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
