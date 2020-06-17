/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Button, GridItem, Grid } from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { MetaDataHeader } from "./MetaDataHeader";
import { MetaDataRow } from "./MetaDataRow";
import { deviceRegistrationTypeOptions } from "modules/iot-device";
import { getInitialStateCreateMetadata } from "modules/iot-device/utils";

export interface ICreateMetadataProps {
  onChangePropertyInput?: (value: string) => Promise<any>;
}

const defaultType =
  deviceRegistrationTypeOptions && deviceRegistrationTypeOptions[0]?.value;

export const CreateMetadata: React.FC<ICreateMetadataProps> = ({
  onChangePropertyInput
}) => {
  const [metadataList, setMetadataList] = useState([
    getInitialStateCreateMetadata(defaultType)
  ]);

  const handleAddParentRow = () => {
    setMetadataList([
      ...metadataList,
      getInitialStateCreateMetadata(defaultType)
    ]);
  };

  return (
    <Grid>
      <GridItem span={12}>
        <MetaDataHeader sectionName="Default properties" />
        {metadataList.map((matadataRow, index: number) => (
          <MetaDataRow
            key={index}
            metadataList={metadataList}
            setMetadataList={setMetadataList}
            rowIndex={index}
          />
        ))}
        <Grid>
          <GridItem span={3}>
            <Button
              id="cd-metadata-buttom-Add-More"
              variant="link"
              icon={<PlusCircleIcon />}
              onClick={handleAddParentRow}
            >
              Add More
            </Button>
          </GridItem>
        </Grid>
      </GridItem>
    </Grid>
  );
};
