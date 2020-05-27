/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { NavLink } from "react-router-dom";
import {
  Modal,
  Button,
  ButtonVariant,
  Form,
  FormGroup,
  Alert
} from "@patternfly/react-core";
import { useStoreContext, types } from "context-state-reducer";
import { PasswordInputFieldWithToggle } from "components";

export const UpdatePassword: React.FC<{}> = () => {
  const [formData, setFormData] = useState({
    password: "",
    retypePassword: ""
  });
  const { state, dispatch } = useStoreContext();
  const { modalProps } = (state && state.modal) || {};
  const { onClose, onConfirm } = modalProps || {};

  const onCloseDialog = () => {
    dispatch({ type: types.HIDE_MODAL });
    onClose && onClose();
  };

  const onConfirmDialog = () => {
    onCloseDialog();
    onConfirm && onConfirm(formData);
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
      return true;
    }
    return false;
  };

  const getHelperText = () => {
    if (!isMatchPassword()) {
      return <span>Passwords don't match. Please try again.</span>;
    }
  };

  return (
    <Modal
      id="update-password-dialog"
      title={"Change password"}
      isSmall={true}
      isOpen={true}
      actions={[
        <Button
          key={"update-password-save-button"}
          variant={ButtonVariant.primary}
          onClick={onConfirmDialog}
          isDisabled={isDisabledSaveButton()}
        >
          Save
        </Button>,
        <Button
          key="update-password-cancel-button"
          variant="link"
          onClick={onCloseDialog}
        >
          Cancel
        </Button>
      ]}
      isFooterLeftAligned={true}
    >
      <Alert variant="info" isInline title="Want to update the secrets?">
        If you want to update the secretsm you can go to the
        {/**
         * TODO: add link path
         */}
        <NavLink id="update-password-edit-credentials-navlink" to="/">
          {" "}
          Edit Credentials{" "}
        </NavLink>
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
            isValid={isMatchPassword()}
          />
        </FormGroup>
      </Form>
    </Modal>
  );
};
