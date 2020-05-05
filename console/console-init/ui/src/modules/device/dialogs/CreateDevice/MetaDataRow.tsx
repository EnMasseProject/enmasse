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

export interface IMetaDataRow {
  onPropertySelect: (e: any, selection: SelectOptionObject) => void;
  onChangePropertyInput?: (value: string) => Promise<any>;
  onPropertyClear: () => void;
  propertySelected?: string;
  propertyInput?: string;
  setPropertyInput?: (value: string) => void;
  formData: any;
  setFormData: (formData: any) => void;
}

export const MetaDataRow: React.FC<IMetaDataRow> = ({
  onPropertySelect,
  onChangePropertyInput,
  onPropertyClear,
  propertySelected,
  propertyInput,
  setPropertyInput,
  formData,
  setFormData
}) => {
  const [currentValue, setCurrentValue] = useState("");
  const [currentKey, setCurrentKey] = useState("");

  const handleAddChildClick = (event: any, parentKey: string) => {
    let element = { defaults: { parentKey: {} }, ext: {} };

    setFormData([...formData, element]);
  };

  const handleMinusClick = (index: any) => {
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

  const dropdownItems = [
    <DropdownItem key="link">Link</DropdownItem>,
    <DropdownItem key="action" component="button">
      Action
    </DropdownItem>,
    <DropdownItem key="disabled link" isDisabled>
      Disabled Link
    </DropdownItem>
  ];
  return (
    <>
      <Grid gutter="sm">
        <GridItem span={5}>
          <InputGroup>
            <TypeAheadSelect
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
              aria-label="popover for input"
              onClick={e => handleAddChildClick(e, "context")}
            >
              <PlusIcon />
            </Button>
          </InputGroup>
        </GridItem>
        <GridItem span={2}>
          <DropdownWithToggle
            id="cas-dropdown-namespace"
            position={DropdownPosition.left}
            onSelectItem={() => {}}
            dropdownItems={dropdownItems}
            value={"String"}
          />
        </GridItem>
        <GridItem span={5}>
          <InputGroup>
            <TextInput
              value={currentValue}
              type="text"
              onChange={handleInputChange}
              aria-label="text input example"
            />
            <Button
              variant="link"
              icon={<MinusCircleIcon />}
              onClick={handleMinusClick}
            />
          </InputGroup>
        </GridItem>
      </Grid>
    </>
  );
};
