/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Modal,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Alert,
  AlertActionLink,
  ValidatedOptions
} from "@patternfly/react-core";
import { useStoreContext, types } from "context-state-reducer";
import { PasswordInputFieldWithToggle } from "components";
import { DeviceActionType } from "modules/iot-device-detail/utils";

export const UpdatePassword = () => {
  const [formData, setFormData] = useState({
    password: "",
    retypePassword: ""
  });
  const { state, dispatch } = useStoreContext();
  const { modalProps } = (state && state.modal) || {};
  const { onClose, onConfirm, credentialProps } = modalProps || {};

  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
    onClose && onClose();
  };

  const onConfirmDialog = () => {
    const { password } = formData;
    onCloseDialog();
    onConfirm && onConfirm({ ...credentialProps, password });
  };

  const isDisabledSaveButton = () => {
    const { password, retypePassword } = formData;
    if (
      password &&
      password.trim() !== "" &&
      retypePassword &&
      retypePassword.trim() !== "" &&
      password === retypePassword
    ) {
      return false;
    }
    return true;
  };

  const onChangeInput = (value: string, evt: any) => {
    const elementName = evt?.target?.name;
    const newFormData = { ...formData };
    (newFormData as any)[elementName] = value;
    setFormData(newFormData);
  };

  const isMatchPassword = () => {
    const { password, retypePassword } = formData;
    if (password.trim() === retypePassword.trim()) {
      return ValidatedOptions.success;
    } else if (password.trim() !== retypePassword.trim()) {
      return ValidatedOptions.error;
    }
    return ValidatedOptions.default;
  };

  const getHelperText = () => {
    if (isMatchPassword() === ValidatedOptions.error) {
      return <span>Passwords don't match. Please try again.</span>;
    }
  };

  const onClickEditCredentials = () => {
    onCloseDialog();
    dispatch({
      type: types.SET_DEVICE_ACTION_TYPE,
      payload: { actionType: DeviceActionType.EDIT_CREDENTIALS }
    });
  };

  return (
    <Modal
      id="update-password-modal"
      title={"Change password"}
      variant="small"
      isOpen={true}
      onClose={onCloseDialog}
      actions={[
        <Button
          id="update-password-save-button"
          aria-label="save updated password"
          key={"update-password-save-button"}
          variant={ButtonVariant.primary}
          onClick={onConfirmDialog}
          isDisabled={isDisabledSaveButton()}
        >
          Save
        </Button>,
        <Button
          id="update-password-cancel-button"
          aria-label="Cancel button"
          key="update-password-cancel-button"
          variant="link"
          onClick={onCloseDialog}
        >
          Cancel
        </Button>
      ]}
    >
      <Alert
        variant="info"
        isInline
        title="Want to update the secrets?"
        actionLinks={
          <Button
            id="update-password-edit-credentials-button"
            aria-label="Edit credentials"
            variant={ButtonVariant.secondary}
            onClick={onClickEditCredentials}
          >
            <AlertActionLink>Edit Credentials</AlertActionLink>
          </Button>
        }
      >
        If you want to update the secrets, you can go to the Edit credentials
        function for advanced setting.
      </Alert>
      <br />
      <Form>
        <FormGroup
          fieldId="update-password-new-password"
          label="Type your new password"
          isRequired
        >
          <PasswordInputFieldWithToggle
            id="update-password-new-password"
            name="password"
            onChange={onChangeInput}
          />
        </FormGroup>
        <FormGroup
          fieldId="update-password-retype-password"
          label="Retype your new password"
          isRequired
          helperText={getHelperText()}
        >
          <PasswordInputFieldWithToggle
            id="update-password-retype-password"
            name="retypePassword"
            onChange={onChangeInput}
            validated={isMatchPassword()}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
