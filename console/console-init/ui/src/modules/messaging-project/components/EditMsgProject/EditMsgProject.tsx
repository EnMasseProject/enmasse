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
import {
  IProject,
  IAuthenticationServiceOptions,
  IPlanOption
} from "modules/project";

export interface IEditMsgProjectProps {
  onCloseDialog: () => void;
  onConfirmDialog: () => void;
  onPlanChange: (plan: string) => void;
  onAuthServiceChange: (authservice: string) => void;
  authServiceOptions: IAuthenticationServiceOptions[];
  planOptions: IPlanOption[];
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
      id="edit-msg-project-modal"
      title="Edit"
      isOpen={true}
      onClose={onCloseDialog}
      actions={[
        <Button
          key="confirm"
          id="edit-msg-project-confirm-button"
          variant="primary"
          onClick={onConfirmDialog}
        >
          Confirm
        </Button>,
        <Button
          key="cancel"
          id="edit-msg-project-cancel-button"
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
          fieldId="edit-msg-project-namespace-select"
          isRequired={true}
        >
          <FormSelect
            id="edit-msg-project-namespace-select"
            isDisabled
            value={project.namespace}
            aria-label="FormSelect Input"
          >
            <FormSelectOption
              id="edit-msg-project-namespace-selectoption"
              key={`project-form-select-option-${project?.namespace}`}
              value={project?.namespace}
              label={project?.namespace || ""}
            />
          </FormSelect>
        </FormGroup>
        <FormGroup
          label="Name"
          fieldId="edit-msg-project-name-input"
          isRequired={true}
        >
          <TextInput
            type="text"
            id="edit-msg-project-name-input"
            isDisabled
            value={project.name}
          />
        </FormGroup>
        <FormGroup
          label="Type"
          fieldId="edit-msg-project-type-radio"
          isInline
          isRequired={true}
        >
          <Radio
            name="radio-1"
            isDisabled
            label="Standard"
            id="edit-msg-project-type-standard-radio"
            key="radio-standard"
            value="standard"
            isChecked={project.type === "standard"}
          />
          <Radio
            name="radio-2"
            isDisabled
            label="Brokered"
            id="edit-msg-project-type-brokered-radio"
            key="radio-standard"
            value="brokered"
            isChecked={project.type === "brokered"}
          />
        </FormGroup>
        <FormGroup
          label="Address space plan"
          fieldId="edit-msg-project-plan-select"
          isRequired={true}
        >
          <FormSelect
            id="edit-msg-project-plan-select"
            value={project.plan}
            onChange={val => onPlanChange(val)}
            aria-label="FormSelect Input"
          >
            {planOptions.map(option => (
              <FormSelectOption
                key={option.value}
                value={option.value}
                label={option.label}
              />
            ))}
          </FormSelect>
        </FormGroup>
        <FormGroup
          label="Authentication Service"
          fieldId="edit-msg-project-auth-service-select"
          isRequired={true}
        >
          <FormSelect
            id="edit-msg-project-auth-service-select"
            value={project.authService}
            onChange={val => onAuthServiceChange(val)}
            aria-label="FormSelect Input"
          >
            {authServiceOptions.map(option => (
              <FormSelectOption
                id={`edit-msg-project-auth-service-selectoption-${option.value}`}
                key={option.value}
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
