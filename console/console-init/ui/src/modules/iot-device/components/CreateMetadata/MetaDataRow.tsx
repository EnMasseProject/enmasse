/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
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
  // console.log("metadataList", metadataList, "type", metadataRow.type);

  const onSelectType = (typeValue: string) => {
    let updatedTypeMetadata = [...metadataList];
    updatedTypeMetadata[rowIndex].type = typeValue;
    if (isObjectOrArray(typeValue as any))
      updatedTypeMetadata[rowIndex].value = "";
    setMetadataList(updatedTypeMetadata);
  };

  //TODO: Call graphql queries and populate options in SelectOption
  const handlePropertyInputChange = async (propertyKey: string) => {
    let updatedPropertyMetadata = [...metadataList];
    updatedPropertyMetadata[rowIndex].key = propertyKey;
    setMetadataList(updatedPropertyMetadata);
  };

  const handleAddChildRow = (event: any, parentKey: string) => {
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
      <Grid gutter="sm">
        <GridItem span={5}>
          <InputGroup>
            <TextInput
              id="cd-metadata-text-parameter"
              value={metadataRow.key}
              type="text"
              onChange={handlePropertyInputChange}
              aria-label="text input example"
            />
            {isObjectOrArray(metadataRow.type) && (
              <Button
                variant="control"
                aria-label="Add child on button click"
                onClick={e => {
                  let parentValue: string = metadataRow.key;
                  handleAddChildRow(e, parentValue);
                }}
              >
                <PlusIcon />
              </Button>
            )}
          </InputGroup>
        </GridItem>
        <GridItem span={2}>
          <DropdownWithToggle
            id="cd-metadata-dropdown-type"
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
              id="cd-metadata-text-value"
              value={metadataRow.value}
              type="text"
              onChange={handleValueChange}
              aria-label="text input example"
              isDisabled={isObjectOrArray(metadataRow.type)}
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
