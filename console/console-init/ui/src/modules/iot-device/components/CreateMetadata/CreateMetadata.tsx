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
  // console.log("metadataList", metadataList);
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
  const getMetadataRows = (metaDataListing: IMetadataProps[]) => {
    console.log("metadataListing", metaDataListing);
    return metaDataListing.map((metadataRow, index: number) => {
      // console.log("metadata row value", metadataRow);
      if (typeof metadataRow.value === "object") {
        console.log("Object Identified");
        getMetadataRows(metadataRow.value as IMetadataProps[]);
      } else {
        return (
          <MetaDataRow
            key={index}
            metadataList={metaDataListing}
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
