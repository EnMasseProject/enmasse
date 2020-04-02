import React from "react";
import {
  TextInput,
  TextInputProps,
  ClipboardCopy,
  ClipboardCopyVariant
} from "@patternfly/react-core";

interface IInputTextProps {
  value?: string | number;
  type?:
    | "number"
    | "time"
    | "text"
    | "tel"
    | "url"
    | "email"
    | "search"
    | "date"
    | "datetime-local"
    | "month"
    | "password";
  setValue?: (value?: string) => void;
  enableCopy?: boolean;
  ariaLabel?: string;
  isReadOnly?: boolean;
  isExpandable?: boolean;
}
const InputText: React.FunctionComponent<IInputTextProps> = ({
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
    <>
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
    </>
  );
};

export { InputText };
