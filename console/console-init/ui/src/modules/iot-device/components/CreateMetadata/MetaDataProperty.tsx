/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { InputGroup, TextInput, Button } from "@patternfly/react-core";
import { isObjectOrArray } from "utils";
import { PlusIcon } from "@patternfly/react-icons";
import { IMetadataProps } from "./CreateMetadata";
import { deviceRegistrationTypeOptions } from "modules/iot-device";
import { uniqueId } from "lodash";

interface IMetaDataPropertyProps {
  metadataRow: any;
  setMetadataList: (metadataRow: any) => void;
  updateMetadataList: (property: string, value: string) => void;
  rowId: string;
  searchMetadataById: (id: string) => number;
}

export const MetaDataProperty: React.FC<IMetaDataPropertyProps> = ({
  metadataRow,
  setMetadataList,
  updateMetadataList,
  rowId,
  searchMetadataById
}) => {
  const currentRow = metadataRow;

  const handlePropertyChange = (property: string) => {
    updateMetadataList("key", property);
  };

  const handleAddChildRow = () => {
    let updatedValueMetadata = [...metadataRow];
    const index = searchMetadataById(rowId);
    let parentKey: string = currentRow.key;
    let newRow: IMetadataProps[] = [
      {
        id: uniqueId(),
        key: parentKey + "/",
        value: "",
        type: deviceRegistrationTypeOptions[0].value
      }
    ];
    updatedValueMetadata[index].value = newRow;
    setMetadataList(updatedValueMetadata);
  };

  return (
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
  );
};
