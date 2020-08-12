/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { TypeAheadSelect, ITypeAheadSelectProps } from "components";
import { mockGatewayGroups } from "mock-data";

export const GatewayGroupTypeAheadSelect: React.FC<ITypeAheadSelectProps> = ({
  id,
  onSelect,
  onClear,
  selected,
  placeholderText = "Input gateway group name",
  isCreatable,
  isMultiple,
  "aria-label": ariaLabel = "gateway group dropdown",
  typeAheadAriaLabel = "typeahead to select gateway group"
}) => {
  const onChangeInput = async (value: string) => {
    // TODO: integrate backend query and remove mock data
    const filtererGroups = mockGatewayGroups?.filter(
      item => item?.value.toLowerCase().indexOf(value.toLowerCase()) > -1
    );
    return filtererGroups;
  };

  return (
    <TypeAheadSelect
      id={id}
      aria-label={ariaLabel}
      aria-describedby="typeahead for gateway groups"
      onSelect={onSelect}
      onClear={onClear}
      selected={selected}
      typeAheadAriaLabel={typeAheadAriaLabel}
      isMultiple={isMultiple}
      onChangeInput={onChangeInput}
      isCreatable={isCreatable}
      placeholderText={placeholderText}
    />
  );
};
