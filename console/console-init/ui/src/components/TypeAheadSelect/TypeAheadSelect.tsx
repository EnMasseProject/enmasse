/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Select,
  SelectVariant,
  SelectProps,
  SelectOptionObject,
  SelectOption
} from "@patternfly/react-core";
import { TYPEAHEAD_REQUIRED_LENGTH, TypeAheadMessage } from "constant";
import { initalSelectOption } from "utils";

export interface ITypeAheadSelectProps extends Omit<SelectProps, "onToggle"> {
  selected?: string;
  inputData?: string;
  onChangeInput?: (value: string) => Promise<any>;
  setInput?: (value: string) => void;
}

const TypeAheadSelect: React.FunctionComponent<ITypeAheadSelectProps> = ({
  ariaLabelTypeAhead,
  ariaLabelledBy,
  onSelect,
  onClear,
  selected,
  inputData,
  placeholderText,
  onChangeInput,
  setInput
}) => {
  const [isExpanded, setIsExpanded] = useState<boolean>(false);
  const [options, setOptions] = useState<any>(initalSelectOption);

  const onToggle = (isExpanded: boolean) => {
    setIsExpanded(isExpanded);
  };

  const onTypeAheadSelect = (e: any, selection: SelectOptionObject) => {
    onSelect && onSelect(e, selection);
    setIsExpanded(false);
  };

  const onFilter = (e: any) => {
    const input = e.target.value && e.target.value.trim();
    setInput && setInput(input);
    if (input.trim().length < TYPEAHEAD_REQUIRED_LENGTH) {
      setOptions([
        <SelectOption
          value={TypeAheadMessage.MORE_CHAR_REQUIRED}
          isDisabled={true}
          key="2"
        />
      ]);
    } else {
      onChangeInput &&
        onChangeInput(input).then((data: any) => {
          const list = data;
          const options = list
            ? list.map((object: any, index: number) => (
                <SelectOption
                  disabled={object.isDisabled}
                  key={index}
                  value={object.value}
                />
              ))
            : [];
          if (options && options.length > 0) {
            setOptions(options);
          } else {
            setOptions([
              <SelectOption
                value={TypeAheadMessage.NO_RESULT_FOUND}
                isDisabled={true}
              />
            ]);
          }
        });
    }

    const options = [
      <SelectOption
        value={TypeAheadMessage.MORE_CHAR_REQUIRED}
        key="1"
        isDisabled={true}
      />
    ];

    return options;
  };

  return (
    <Select
      variant={SelectVariant.typeahead}
      ariaLabelTypeAhead={ariaLabelTypeAhead}
      onToggle={onToggle}
      onSelect={onTypeAheadSelect}
      onClear={onClear}
      onFilter={onFilter}
      selections={selected || inputData}
      isExpanded={isExpanded}
      ariaLabelledBy={ariaLabelledBy}
      placeholderText={placeholderText}
    >
      {options}
    </Select>
  );
};

export { TypeAheadSelect };
