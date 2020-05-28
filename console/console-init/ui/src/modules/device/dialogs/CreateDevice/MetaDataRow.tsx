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
  SelectOptionObject,
  SelectVariant,
  Select,
  SelectOption
} from "@patternfly/react-core";
import { DropdownWithToggle } from "components";
import { PlusIcon, MinusCircleIcon } from "@patternfly/react-icons";
import { deviceRegistrationTypeOptions, getLabelByValue } from "modules/device";
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
  const [propertySelected, setPropertySelected] = useState<string | undefined>(
    ""
  );
  const [isExpanded, setIsExpanded] = useState<boolean>(false);

  const onToggle = (isExpanded: boolean) => {
    setIsExpanded(isExpanded);
  };

  const onTypeAheadSelect = (e: any, selection: SelectOptionObject) => {
    onPropertySelect && onPropertySelect(e, selection);
    setIsExpanded(false);
  };

  const onSelectType = (typeValue: string) => {
    let updatedTypeMetadata = [...metadataList];
    updatedTypeMetadata[rowIndex].type = typeValue;
    setMetadataList(updatedTypeMetadata);
  };

  //TODO: Move this method to container
  const onFilter = (e: any) => {
    const propertyInput = e.target.value && e.target.value.trim();
    //TODO: Analyze if option type can be changed
    let propertyOptions: React.ReactElement[] = [];
    onChangePropertyInput &&
      onChangePropertyInput(propertyInput).then((options: any) => {
        const propertyList = options;
        propertyOptions = propertyList
          ? propertyList.map((propertyItem: any, index: number) => (
              <SelectOption
                disabled={propertyItem.isDisabled}
                key={index}
                value={propertyItem.value}
              />
            ))
          : [];
      });

    return propertyOptions;
  };

  //TODO: Call graphql queries and populate options in SelectOption
  const onChangePropertyInput = async (propertyKey: string) => {
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

  const onPropertySelect = (e: any, selection: SelectOptionObject) => {
    setPropertySelected(selection.toString());
  };

  const onPropertyClear = () => {
    setPropertySelected(undefined);
    let updatedPropertyMetadata = [...metadataList];
    updatedPropertyMetadata[rowIndex].key = "";
    setMetadataList(updatedPropertyMetadata);
  };

  //TODO: Clear value of property when type is array or object
  //TODO: Disable Type when type is array or object

  const isChildAdditionEnabled = (type: DataType.ARRAY | DataType.OBJECT) => {
    return type === DataType.OBJECT || type === DataType.ARRAY;
  };

  //TODO: Increase width of type dropdown
  return (
    <>
      <Grid gutter="sm">
        <GridItem span={5}>
          <InputGroup>
            <Select
              id="cd-metadata-typeahead-parameter"
              variant={SelectVariant.typeahead}
              ariaLabelTypeAhead={"Select parameter"}
              onToggle={onToggle}
              onSelect={onTypeAheadSelect}
              onClear={onPropertyClear}
              selections={propertySelected || metadataRow.key}
              isExpanded={isExpanded}
              onFilter={onFilter}
              ariaLabelledBy={"typeahead-parameter-id"}
              placeholderText={"Select property"}
            ></Select>
            {metadataRow.length > 0 ? (
              isChildAdditionEnabled(metadataRow.type) && (
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
              )
            ) : (
              <></>
            )}
          </InputGroup>
        </GridItem>
        <GridItem span={2}>
          <DropdownWithToggle
            id="cd-metadata-dropdown-type"
            position={DropdownPosition.left}
            onSelectItem={onSelectType}
            dropdownItems={deviceRegistrationTypeOptions}
            value={getLabelByValue(metadataRow.type)}
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
              isDisabled={isChildAdditionEnabled(metadataRow.type)}
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
