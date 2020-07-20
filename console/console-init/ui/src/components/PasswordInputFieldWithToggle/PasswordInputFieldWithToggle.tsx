/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  TextInput,
  TextInputProps,
  ValidatedOptions,
  TextInputTypes
} from "@patternfly/react-core";
import { EyeIcon, EyeSlashIcon } from "@patternfly/react-icons";
import { StyleSheet, css } from "aphrodite";

const styles = StyleSheet.create({
  icon: {
    minWidth: 35
  },
  textInput: {
    marginRight: -35
  }
});

export interface IPasswordInputFieldWithToggleProps extends TextInputProps {
  id: string;
}

export const PasswordInputFieldWithToggle: React.FC<IPasswordInputFieldWithToggleProps> = ({
  id,
  onChange,
  name,
  validated
}) => {
  const [shouldShowPassword, setshouldShowPassword] = useState<boolean>(false);

  const onToggle = (showPassword: boolean) => {
    setshouldShowPassword(showPassword);
  };

  const renderIcon = () => {
    if (validated !== ValidatedOptions.error) {
      if (shouldShowPassword) {
        return (
          <EyeSlashIcon
            id="password-input-eyeslashicon"
            className={css(styles.icon)}
            onClick={() => onToggle(false)}
          />
        );
      }
      return (
        <EyeIcon
          id="password-input-eyeicon"
          className={css(styles.icon)}
          onClick={() => onToggle(true)}
        />
      );
    }
  };

  const type = shouldShowPassword
    ? TextInputTypes.text
    : TextInputTypes.password;

  return (
    <>
      <TextInput
        className={css(styles.textInput)}
        id={id}
        name={name}
        type={type}
        onChange={onChange}
        validated={
          validated === ValidatedOptions.error
            ? ValidatedOptions.error
            : ValidatedOptions.default
        }
      />
      {renderIcon()}
    </>
  );
};
