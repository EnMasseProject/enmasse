/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Grid, GridItem } from "@patternfly/react-core";
import { ValidationStatusType } from "modules/iot-device/utils";
import { isObjectOrArray } from "utils";
import { MetaDataProperty } from "./MetaDataProperty";
import { MetaDataType } from "./MetaDataType";
import { MetaDataValue } from "./MetaDataValue";

export interface IMetaDataRowProps {
  metadataRow: any;
  metadataList: any;
  setMetadataList: (metadataRow: any) => void;
  rowId: string;
}

export const MetaDataRow: React.FC<IMetaDataRowProps> = ({
  metadataRow,
  metadataList,
  setMetadataList,
  rowId
}) => {
  const [validationStatus, setValidationStatus] = useState<
    ValidationStatusType.DEFAULT | ValidationStatusType.ERROR
  >(ValidationStatusType.DEFAULT);

  const updateMetadataList = (property: string, value: string) => {
    let updatedMetadataList = [...metadataList];
    const index = searchMetadataById(rowId);
    updatedMetadataList[index][property] = value;
    if (property === "type" && isObjectOrArray(value as any))
      updatedMetadataList[0].value = [];
    setMetadataList(updatedMetadataList);
  };

  const searchMetadataById = (id: string) => {
    let updatedMetadataList = [...metadataList];
    let index = -1;
    updatedMetadataList.forEach((metadataRowItem, rowIndex) => {
      if (metadataRowItem.id === id) {
        index = rowIndex;
      }
    });
    return index;
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

  return (
    <>
      <Grid hasGutter>
        <GridItem span={5}>
          <MetaDataProperty
            metadataRow={metadataRow}
            setMetadataList={setMetadataList}
            updateMetadataList={updateMetadataList}
            rowId={rowId}
            searchMetadataById={searchMetadataById}
          />
        </GridItem>
        <GridItem span={2}>
          <MetaDataType
            metadataRow={metadataRow}
            getValidationStatus={getValidationStatus}
            setValidationStatus={setValidationStatus}
            updateMetadataList={updateMetadataList}
          />
        </GridItem>
        <GridItem span={5}>
          <MetaDataValue
            metadataRow={metadataRow}
            getValidationStatus={getValidationStatus}
            setValidationStatus={setValidationStatus}
            updateMetadataList={updateMetadataList}
            setMetadataList={setMetadataList}
            validationStatus={validationStatus}
          />
        </GridItem>
      </Grid>
    </>
  );
};
