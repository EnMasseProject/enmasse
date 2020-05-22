/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { TextInput, TextInputProps } from "@patternfly/react-core";
import { StyleSheet } from "@patternfly/react-styles";
import classNames from "classnames";

export interface IPasswordLabelProps extends TextInputProps {
  id: string;
}

const styles = StyleSheet.create({
  textinput: {
    fontSize: 20,
    backgroundColor: "var(--pf-global--palette--white)"
  }
});

export const PasswordLabel: React.FC<IPasswordLabelProps> = ({
  id,
  className,
  value
}) => {
  const cssClass = classNames(className, styles.textinput);

  return (
    <TextInput
      id={id}
      type="password"
      isDisabled
      value={value}
      className={cssClass}
    />
  );
};
