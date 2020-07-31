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

interface IMetaDataPropertyProps {
  metadataList: any;
  setMetadataList: (metadataList: any) => void;
  rowIndex: number;
  updateMetadataList: (property: string, value: string) => void;
}

export const MetaDataProperty: React.FC<IMetaDataPropertyProps> = ({
  metadataList,
  setMetadataList,
  rowIndex,
  updateMetadataList,
}) => {
  const currentRow = metadataList[rowIndex];

  const handlePropertyChange = (property: string) => {
    updateMetadataList("key", property);
  };

  const handleAddChildRow = () => {
    let parentKey: string = currentRow.key;
    let newRow: IMetadataProps = {
      key: parentKey + "/",
      value: "",
      type: deviceRegistrationTypeOptions[0].value,
    };
    let updatedValueMetadata = [...metadataList];
    updatedValueMetadata[rowIndex].value = newRow;
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
