/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Grid, GridItem } from "@patternfly/react-core";
import { ValidationStatusType } from "modules/iot-device/utils";
import { isObjectOrArray } from "utils";
import { MetadataProperty } from "./MetadataProperty";
import { MetadataType } from "./MetadataType";
import { MetadataValue } from "./MetadataValue";

export interface IMetaDataRowProps {
  metadataRow: any;
  metadataList: any;
  setMetadataList: (metadataRow: any) => void;
  rowId: string;
}

export const MetadataRow: React.FC<IMetaDataRowProps> = ({
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
    const index = findMetadataIndexById(rowId);
    updatedMetadataList[index][property] = value;
    if (property === "type" && isObjectOrArray(value as any))
      updatedMetadataList[0].value = [];
    setMetadataList(updatedMetadataList);
  };

  const findMetadataIndexById = (id: string) => {
    let updatedMetadataList = [...metadataList];
    let metadataIndex = updatedMetadataList.findIndex(
      metadataRowItem => metadataRowItem.id === id
    );
    return metadataIndex;
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
          <MetadataProperty
            metadataRow={metadataRow}
            setMetadataList={setMetadataList}
            updateMetadataList={updateMetadataList}
            rowId={rowId}
            findMetadataIndexById={findMetadataIndexById}
          />
        </GridItem>
        <GridItem span={2}>
          <MetadataType
            metadataRow={metadataRow}
            getValidationStatus={getValidationStatus}
            setValidationStatus={setValidationStatus}
            updateMetadataList={updateMetadataList}
          />
        </GridItem>
        <GridItem span={5}>
          <MetadataValue
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
