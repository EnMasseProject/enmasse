import React, { useState, useEffect } from "react";
import {
  Select,
  SelectVariant,
  SelectOption,
  SelectOptionObject
} from "@patternfly/react-core";
import {
  TypeAheadMessage,
  TYPEAHEAD_REQUIRED_LENGTH
} from "constants/constants";
import { ISelectOption } from "utils";

interface ITypeAheadSelectProps {
  id: string;
  ariaLabel: string;
  ariaLabelledBy: string;
  placeholderText: string;
  options?: Array<ISelectOption>;
  selected?: string;
  setSelected: (value?: string) => void;
  inputData: string;
  setInputData: (value: string) => void;
  onChangeInputData: (value: string) => void;
}
const TypeAheadSelect: React.FunctionComponent<ITypeAheadSelectProps> = ({
  id,
  ariaLabel,
  ariaLabelledBy,
  placeholderText,
  options,
  selected,
  setSelected,
  inputData,
  setInputData,
  onChangeInputData
}) => {
  const [isExpanded, setIsExpanded] = useState<boolean>();
  const [selectOptions, setSelectOptions] = useState<any>(options);

  useEffect(() => {
    setSelectOptions(options);
  }, [options]);

  const onToggle = (isExpanded: boolean) => {
    setIsExpanded(isExpanded);
  };

  const onSelect = (event: any, selection: string | SelectOptionObject) => {
    setSelected(selection.toString());
    setIsExpanded(false);
  };

  const onClear = () => {
    setInputData("");
    setSelected(undefined);
    setIsExpanded(false);
  };

  const onFilter = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setInputData(value);
    onChangeInputData(value);
    const dropdownOptions: React.ReactElement[] = options
      ? options.map((option, index) => (
          <SelectOption key={index} value={option} />
        ))
      : [];
    setSelectOptions(dropdownOptions);
    return dropdownOptions;
  };

  return (
    <Select
      id={id}
      variant={SelectVariant.typeahead}
      aria-label={ariaLabel}
      onToggle={onToggle}
      onSelect={onSelect}
      onClear={onClear}
      maxHeight="200px"
      selections={selected || inputData}
      onFilter={onFilter}
      isExpanded={isExpanded}
      ariaLabelledBy={ariaLabelledBy}
      placeholderText={placeholderText}
      isDisabled={false}
      isCreatable={false}
    >
      {selectOptions && selectOptions.length > 0 ? (
        selectOptions.map((option: any, index: number) => (
          <SelectOption
            key={index}
            value={option.value || option.props.value}
            isDisabled={option.isDisabled}
          />
        ))
      ) : inputData.trim().length < TYPEAHEAD_REQUIRED_LENGTH ? (
        <SelectOption
          key={"invalid-input-length"}
          value={TypeAheadMessage.MORE_CHAR_REQUIRED}
          disabled={true}
        />
      ) : (
        <SelectOption
          key={"no-results-found"}
          value={TypeAheadMessage.NO_RESULT_FOUND}
          disabled={true}
        />
      )}
      {/* {} */}
    </Select>
  );
};

export { TypeAheadSelect };
