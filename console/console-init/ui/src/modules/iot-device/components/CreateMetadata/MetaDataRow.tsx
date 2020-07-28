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
import { deviceRegistrationTypeOptions } from "modules/iot-device";
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
  const metadataRow = metadataList[rowIndex];
  const [validationStatus, setValidationStatus] = useState<
    ValidationStatusType.DEFAULT | ValidationStatusType.ERROR
  >(ValidationStatusType.DEFAULT);

  const onSelectType = (typeValue: string) => {
    const validationStatus = getValidationStatus(typeValue, metadataRow.value);
    setValidationStatus(validationStatus);
    let updatedTypeMetadata = [...metadataList];
    updatedTypeMetadata[rowIndex].type = typeValue;
    if (isObjectOrArray(typeValue as any))
      updatedTypeMetadata[rowIndex].value = "";
    setMetadataList(updatedTypeMetadata);
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

  //TODO: Call graphql queries and populate options in SelectOption
  const handlePropertyInputChange = async (propertyKey: string) => {
    let updatedPropertyMetadata = [...metadataList];
    updatedPropertyMetadata[rowIndex].key = propertyKey;
    setMetadataList(updatedPropertyMetadata);
  };

  const handleAddChildRow = (event: any) => {
    let parentKey: string = metadataRow.key;
    let newRow = {
      key: parentKey + "/",
      value: [],
      type: deviceRegistrationTypeOptions[0].value
    };
    setMetadataList([...metadataList, newRow]);
  };

  const handleDeleteRow = (index: any) => {
    const deletedRowMetadata = [...metadataList];
    deletedRowMetadata.splice(index, 1);
    setMetadataList(deletedRowMetadata);
  };

  const handleValueChange = (propertyValue: string, e: any) => {
    const validationStatus = getValidationStatus(
      metadataRow.type,
      propertyValue
    );
    setValidationStatus(validationStatus);
    let updatedValueMetadata = [...metadataList];
    updatedValueMetadata[rowIndex].value = propertyValue;
    setMetadataList(updatedValueMetadata);
  };

  const isObjectOrArray = (type: DataType.ARRAY | DataType.OBJECT) => {
    return type === DataType.OBJECT || type === DataType.ARRAY;
  };

  //TODO: Increase width of type dropdown
  return (
    <>
      <Grid hasGutter>
        <GridItem span={5}>
          <InputGroup>
            <TextInput
              id="metadata-row-text-parameter-input"
              value={metadataRow.key}
              type="text"
              onChange={handlePropertyInputChange}
              aria-label="text input example"
            />
            {isObjectOrArray(metadataRow.type) && (
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
            onSelectItem={onSelectType}
            dropdownItems={deviceRegistrationTypeOptions}
            value={metadataRow.type}
            isLabelAndValueNotSame={true}
            isDisabled={isObjectOrArray(metadataRow.type)}
          />
        </GridItem>
        <GridItem span={5}>
          <InputGroup>
            <TextInput
              id="metadat-row-text-value-input"
              value={metadataRow.value}
              validated={validationStatus}
              type="text"
              onChange={handleValueChange}
              aria-label="text input example"
              isDisabled={isObjectOrArray(metadataRow.type)}
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
