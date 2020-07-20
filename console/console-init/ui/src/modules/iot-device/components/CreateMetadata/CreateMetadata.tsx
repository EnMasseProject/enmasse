/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Button, GridItem, Grid } from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { MetaDataHeader } from "./MetaDataHeader";
import { MetaDataRow } from "./MetaDataRow";
import { getInitialMetadataState } from "modules/iot-device/utils";

export const CreateMetadata: React.FC = () => {
  const [metadataList, setMetadataList] = useState([getInitialMetadataState()]);

  const handleAddParentRow = () => {
    setMetadataList([...metadataList, getInitialMetadataState()]);
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
