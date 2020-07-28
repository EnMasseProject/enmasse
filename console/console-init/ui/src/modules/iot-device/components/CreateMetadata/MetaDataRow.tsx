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
  TextInput
} from "@patternfly/react-core";
import { DropdownWithToggle } from "components";
import { PlusIcon, MinusCircleIcon } from "@patternfly/react-icons";
import {
  deviceRegistrationTypeOptions,
  IMetadataProps
} from "modules/iot-device";
import { DataType } from "constant";
import { ValidationStatusType } from "modules/iot-device/utils";

export interface IMetaDataRow {
  metadataList: any;
  setMetadataList: (metadataList: any) => void;
  rowIndex: number;
}

export const MetaDataRow: React.FC<IMetaDataRow> = ({
  metadataList,
  setMetadataList,
  rowIndex
}) => {
  const currentRow = metadataList[rowIndex];
  const [validationStatus, setValidationStatus] = useState<
    ValidationStatusType.DEFAULT | ValidationStatusType.ERROR
  >(ValidationStatusType.DEFAULT);

  const updateMetadataList = (property: string, value: string) => {
    let updatedMetadataList = [...metadataList];
    switch (property) {
      case "key":
        updatedMetadataList[rowIndex].key = value;
        break;
      case "type":
        updatedMetadataList[rowIndex].type = value;
        if (isObjectOrArray(value as any))
          updatedMetadataList[rowIndex].value = "";
        break;
      case "value":
        updatedMetadataList[rowIndex].value = value;
        break;
    }
    setMetadataList(updatedMetadataList);
  };

  const handleTypeChange = (type: string) => {
    const validationStatus = getValidationStatus(type, currentRow.value);
    setValidationStatus(validationStatus);
    updateMetadataList("type", type);
  };

  const handlePropertyChange = (property: string) => {
    updateMetadataList("key", property);
  };

  const handleValueChange = (value: string, _: any) => {
    const validationStatus = getValidationStatus(currentRow.type, value);
    setValidationStatus(validationStatus);
    updateMetadataList("value", value);
  };

  const isObjectOrArray = (type: DataType.ARRAY | DataType.OBJECT) => {
    return type === DataType.OBJECT || type === DataType.ARRAY;
  };

  const getValidationStatus = (type: string, value: string) => {
    let validationStatus:
      | ValidationStatusType.DEFAULT
      | ValidationStatusType.ERROR = ValidationStatusType.DEFAULT;
    switch (type) {
      case "string":
        validationStatus =
          typeof value === "string"
            ? ValidationStatusType.DEFAULT
            : ValidationStatusType.ERROR;
        break;
      case "number":
        validationStatus = !isNaN(Number(value))
          ? ValidationStatusType.DEFAULT
          : ValidationStatusType.ERROR;
        break;
      case "boolean":
        validationStatus =
          value &&
          (value?.toLowerCase() === "true" || value?.toLowerCase() === "false")
            ? ValidationStatusType.DEFAULT
            : ValidationStatusType.ERROR;
        break;
      case "datetime":
        validationStatus = !isNaN(Date.parse(value))
          ? ValidationStatusType.DEFAULT
          : ValidationStatusType.ERROR;
        break;
      default:
    }
    return validationStatus;
  };

  const handleAddChildRow = (_: any) => {
    let parentKey: string = currentRow.key;
    let newRow: IMetadataProps = {
      key: parentKey + "/",
      value: "",
      type: deviceRegistrationTypeOptions[0].value
    };
    let updatedValueMetadata = [...metadataList];
    updatedValueMetadata[rowIndex].value = newRow;
    setMetadataList(updatedValueMetadata);
  };

  const handleDeleteRow = (index: any) => {
    const deletedRowMetadata = [...metadataList];
    deletedRowMetadata.splice(index, 1);
    setMetadataList(deletedRowMetadata);
  };

  //TODO: Increase width of type dropdown
  return (
    <>
      <Grid hasGutter>
        <GridItem span={5}>
          <InputGroup>
            <TextInput
              id="metadata-row-text-parameter-input"
              value={currentRow.key}
              type="text"
              onChange={handlePropertyChange}
              aria-label="text input example"
            />
            {isObjectOrArray(currentRow.type) && (
              <Button
                id="metadata-row-add-child-button"
                variant="control"
                aria-label="Add child on button click"
                onClick={handleAddChildRow}
              >
                <PlusIcon />
              </Button>
            )}
          </InputGroup>
        </GridItem>
        <GridItem span={2}>
          <DropdownWithToggle
            id="metadata-row-type-dropdowntoggle"
            toggleId="metadata-row-type-dropdown-toggle"
            position={DropdownPosition.left}
            onSelectItem={handleTypeChange}
            dropdownItems={deviceRegistrationTypeOptions}
            value={currentRow.type}
            isLabelAndValueNotSame={true}
            isDisabled={isObjectOrArray(currentRow.type)}
          />
        </GridItem>
        <GridItem span={5}>
          <InputGroup>
            <TextInput
              id="metadata-row-text-value-input"
              value={currentRow.value}
              validated={validationStatus}
              type="text"
              onChange={handleValueChange}
              aria-label="text input example"
              isDisabled={isObjectOrArray(currentRow.type)}
            />
            <Button
              id="metadata-row-delete-button"
              aria-label="delete button"
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
