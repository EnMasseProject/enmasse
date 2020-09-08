/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { InputGroup, TextInput, Button } from "@patternfly/react-core";
import { isObjectOrArray } from "utils";
import { MinusCircleIcon } from "@patternfly/react-icons";
import { ValidationStatusType } from "modules/iot-device";

interface IMetadataValueProps {
  metadataRow: any;
  updateMetadataList: (property: string, value: string) => void;
  getValidationStatus: (type: string, value: string) => ValidationStatusType;
  setValidationStatus: (value: ValidationStatusType) => void;
  setMetadataList: (metadataRow: any) => void;
  validationStatus: ValidationStatusType;
}

export const MetadataValue: React.FC<IMetadataValueProps> = ({
  metadataRow,
  updateMetadataList,
  getValidationStatus,
  setValidationStatus,
  setMetadataList,
  validationStatus
}) => {
  const currentRow = metadataRow;

  const handleValueChange = (value: string) => {
    const validationStatus = getValidationStatus(currentRow.type, value);
    setValidationStatus(validationStatus);
    updateMetadataList("value", value);
  };

  const handleDeleteRow = (index: any) => {
    const deletedRowMetadata = [...metadataRow];
    deletedRowMetadata.splice(index, 1);
    setMetadataList(deletedRowMetadata);
  };

  return (
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
  );
};
