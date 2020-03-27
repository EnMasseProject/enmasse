import React, { useState } from "react";
import {
  Select,
  SelectVariant,
  SelectProps,
  SelectOptionObject
} from "@patternfly/react-core";

export interface ITypeAheadSelectProps extends Omit<SelectProps, "onToggle"> {
  selected?: string;
  inputData?: string;
  options?: any[];
}

const TypeAhead: React.FunctionComponent<ITypeAheadSelectProps> = ({
  ariaLabelTypeAhead,
  ariaLabelledBy,
  onSelect,
  onClear,
  onFilter,
  selected,
  inputData,
  options,
  placeholderText
}) => {
  const [isExpanded, setIsExpanded] = useState<boolean>(false);
  const onTypeAheadToggle = () => {
    setIsExpanded(!isExpanded);
  };
  const onTypeAheadSelect = (e: any, selection: SelectOptionObject) => {
    onSelect && onSelect(e, selection);
    setIsExpanded(false);
  };

  return (
    <Select
      variant={SelectVariant.typeahead}
      ariaLabelTypeAhead={ariaLabelTypeAhead}
      onToggle={onTypeAheadToggle}
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

export { TypeAhead };
