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
  metadataList: any;
  setMetadataList: (metadataList: any) => void;
  rowIndex: number;
}

export const MetaDataRow: React.FC<IMetaDataRowProps> = ({
  metadataList,
  setMetadataList,
  rowIndex
}) => {
  const [validationStatus, setValidationStatus] = useState<
    ValidationStatusType.DEFAULT | ValidationStatusType.ERROR
  >(ValidationStatusType.DEFAULT);

  const updateMetadataList = (property: string, value: string) => {
    let updatedMetadataList = [...metadataList];
    updatedMetadataList[rowIndex][property] = value;
    if (property === "type" && isObjectOrArray(value as any))
      updatedMetadataList[rowIndex].value = "";
    setMetadataList(updatedMetadataList);
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

  //TODO: Increase width of type dropdown
  return (
    <>
      <Grid hasGutter>
        <GridItem span={5}>
          <MetaDataProperty
            metadataList={metadataList}
            setMetadataList={setMetadataList}
            rowIndex={rowIndex}
            updateMetadataList={updateMetadataList}
          />
        </GridItem>
        <GridItem span={2}>
          <MetaDataType
            metadataList={metadataList}
            rowIndex={rowIndex}
            getValidationStatus={getValidationStatus}
            setValidationStatus={setValidationStatus}
            updateMetadataList={updateMetadataList}
          />
        </GridItem>
        <GridItem span={5}>
          <MetaDataValue
            metadataList={metadataList}
            rowIndex={rowIndex}
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
