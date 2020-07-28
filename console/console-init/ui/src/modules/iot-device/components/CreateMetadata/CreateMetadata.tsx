/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Button, GridItem, Grid } from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { MetaDataHeader } from "./MetaDataHeader";
import { MetaDataRow } from "./MetaDataRow";
import { deviceRegistrationTypeOptions } from "modules/iot-device/utils";

export interface IMetadataProps {
  key: string;
  type: string;
  value: string | IMetadataProps[];
}

const getInitialMetadataState: IMetadataProps[] = [
  {
    key: "",
    value: "",
    type: deviceRegistrationTypeOptions[0].value
  }
];

export const CreateMetadata: React.FC = () => {
  const [metadataList, setMetadataList] = useState<IMetadataProps[]>(
    getInitialMetadataState
  );
  const handleAddParentRow = () => {
    setMetadataList([
      ...metadataList,
      {
        key: "",
        value: "",
        type: deviceRegistrationTypeOptions[0].value
      }
    ]);
  };
  //TODO: Add child rows
  const getMetadataRows = (metaDataRows: IMetadataProps[]) => {
    return metaDataRows.map((metadataRow, index: number) => {
      if (typeof metadataRow.value === "object") {
        getMetadataRows(metadataRow.value as IMetadataProps[]);
      } else {
        return (
          <MetaDataRow
            key={index}
            metadataList={metaDataRows}
            setMetadataList={setMetadataList}
            rowIndex={index}
          />
        );
      }
    });
  };

  return (
    <Grid>
      <GridItem span={12}>
        <MetaDataHeader sectionName="Default properties" />
        {getMetadataRows(metadataList)}
        <Grid>
          <GridItem span={3}>
            <Button
              id="create-metadata-add-more-button"
              aria-label="add more"
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
