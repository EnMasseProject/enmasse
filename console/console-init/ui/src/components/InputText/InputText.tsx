/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  TextInput,
  ClipboardCopy,
  ClipboardCopyVariant,
  Grid,
  GridItem,
  TextInputProps
} from "@patternfly/react-core";

interface IInputTextProps extends TextInputProps {
  setValue?: (value?: string) => void;
  enableCopy?: boolean;
  ariaLabel?: string;
  isExpandable?: boolean;
}
const InputText: React.FunctionComponent<IInputTextProps> = ({
  label,
  type,
  value,
  setValue,
  isReadOnly,
  enableCopy,
  ariaLabel,
  isExpandable
}) => {
  const onTextChange = (text?: string | number) => {
    setValue && setValue(text + "");
  };
  const onChange = (value?: string) => {
    setValue && setValue(value + "");
  };
  return (
    <Grid>
      <GridItem span={4}>
        <b>{label}</b>
      </GridItem>
      <GridItem span={8}>
        {enableCopy ? (
          <>
            <ClipboardCopy
              variant={
                isExpandable
                  ? ClipboardCopyVariant.expansion
                  : ClipboardCopyVariant.inline
              }
              onChange={onTextChange}
              type={type}
              isReadOnly={isReadOnly}
            >
              {value}
            </ClipboardCopy>
          </>
        ) : (
          <TextInput
            value={value}
            type={type}
            onChange={onChange}
            isReadOnly={isReadOnly}
            aria-label={ariaLabel}
          />
        )}
      </GridItem>
    </Grid>
  );
};

export { InputText };
