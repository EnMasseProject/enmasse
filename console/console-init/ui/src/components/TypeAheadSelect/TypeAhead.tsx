import React, { useState } from "react";
import {
  Select,
  SelectVariant,
  SelectProps,
  SelectOptionObject
} from "@patternfly/react-core";
import { placeholder } from "@babel/types";

interface ITypeAheadSelectProps {
  ariaLabelTypeAhead?: string;
  ariaLabelledBy?: string;
  onSelect: (
    event: React.MouseEvent | React.ChangeEvent,
    value: string | SelectOptionObject,
    isPlaceholder?: boolean
  ) => void;
  onClear: (event: React.MouseEvent) => void;
  onFilter?: (e: React.ChangeEvent<HTMLInputElement>) => React.ReactElement[];
  selected?: string;
  inputData?: string;
  options?: any[];
  placeholderText?: string;
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
    // setSelected(selection.toString());
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
