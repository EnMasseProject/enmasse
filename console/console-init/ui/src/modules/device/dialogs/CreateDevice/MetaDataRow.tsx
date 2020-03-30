/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Grid,
  GridItem,
  InputGroup,
  Button,
  DropdownPosition,
  TextInput,
  DropdownItem,
  SelectOptionObject
} from "@patternfly/react-core";
import { TypeAheadSelect, DropdownWithToggle } from "components";
import { PlusIcon, MinusCircleIcon } from "@patternfly/react-icons";
import { ISelectOption } from "utils";

export interface IMetaDataRow {
  onChangePropertyInput?: (value: string) => Promise<any>;
  formData: any;
  setFormData: (formData: any) => void;
}

export const MetaDataRow: React.FC<IMetaDataRow> = ({
  onChangePropertyInput,
  formData,
  setFormData
}) => {
  const [currentValue, setCurrentValue] = useState("");
  const [currentKey, setCurrentKey] = useState("");
  const [propertySelected, setPropertySelected] = useState<string | undefined>(
    ""
  );
  const [propertyInput, setPropertyInput] = useState<string | undefined>("");

  const handleAddChildRow = (event: any, parentKey: string) => {
    let element = { defaults: { parentKey: {} }, ext: {} };
    setFormData([...formData, element]);
  };

  const handleDeleteRow = (index: any) => {
    const metadataList = [...formData];
    metadataList.splice(index, 1);
    setFormData(metadataList);
  };

  const handleInputChange = (e: any, index: any) => {
    const { name, value } = e.target;
    let metadataList = [...formData];
    // metadataList[index][name]=value;
    setFormData(metadataList);
  };

  const onPropertySelect = (e: any, selection: SelectOptionObject) => {
    setPropertySelected(selection.toString());
    setPropertyInput(undefined);
  };

  const onPropertyClear = () => {
    setPropertySelected(undefined);
    setPropertyInput(undefined);
  };

  const typeOptions: ISelectOption[] = [
    { key: "string", label: "String", value: "String" },
    { key: "number", label: "Number", value: "Number" },
    { key: "boolean", label: "Boolean", value: "Boolean" }
  ];

  return (
    <>
      <Grid gutter="sm">
        <GridItem span={5}>
          <InputGroup>
            <TypeAheadSelect
              id="cd-metadata-typeahead-parameter"
              ariaLabelTypeAhead={"Select parameter"}
              ariaLabelledBy={"typeahead-parameter-id"}
              onSelect={onPropertySelect}
              onClear={onPropertyClear}
              selected={propertySelected}
              inputData={propertyInput || ""}
              placeholderText={"Select property"}
              onChangeInput={onChangePropertyInput}
              setInput={setPropertyInput}
            />
            <Button
              variant="control"
              aria-label="Add child on button click"
              onClick={e => handleAddChildRow(e, "context")}
            >
              <PlusIcon />
            </Button>
          </InputGroup>
        </GridItem>
        <GridItem span={2}>
          <DropdownWithToggle
            id="cd-metadata-dropdown-type"
            position={DropdownPosition.left}
            onSelectItem={() => {}}
            dropdownItems={typeOptions}
            value={"String"}
          />
        </GridItem>
        <GridItem span={5}>
          <InputGroup>
            <TextInput
              id="cd-metadata-text-value"
              value={currentValue}
              type="text"
              onChange={handleInputChange}
              aria-label="text input example"
            />
            <Button
              id="cd-metadata-button-delete"
              variant="link"
              icon={<MinusCircleIcon />}
              onClick={handleDeleteRow}
            />
          </InputGroup>
        </GridItem>
      </Grid>
    </>
  );
};
