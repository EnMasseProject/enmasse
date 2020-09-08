/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { InputGroup, TextInput, Button } from "@patternfly/react-core";
import { isObjectOrArray } from "utils";
import { PlusIcon } from "@patternfly/react-icons";
import { IMetadataProps } from "./CreateMetadata";
import { getInitialMetadataState } from "modules/iot-device";

interface IMetadataPropertyProps {
  metadataRow: any;
  setMetadataList: (metadataRow: any) => void;
  updateMetadataList: (property: string, value: string) => void;
  rowId: string;
  findMetadataIndexById: (id: string) => number;
}

export const MetadataProperty: React.FC<IMetadataPropertyProps> = ({
  metadataRow,
  setMetadataList,
  updateMetadataList,
  rowId,
  findMetadataIndexById
}) => {
  const currentRow = metadataRow;

  const handlePropertyChange = (property: string) => {
    updateMetadataList("key", property);
  };

  const handleAddChildRow = () => {
    let updatedValueMetadata = [...metadataRow];
    const index = findMetadataIndexById(rowId);
    let newRow: IMetadataProps[] = getInitialMetadataState;
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
