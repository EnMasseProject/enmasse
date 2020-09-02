/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState, useEffect } from "react";
import { Button, GridItem, Grid } from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { MetaDataRow } from "./MetaDataRow";
import { deviceRegistrationTypeOptions } from "modules/iot-device/utils";
import { uniqueId } from "lodash";

export interface IMetadataProps {
  id: string;
  key: string;
  type: string;
  value: any;
}

export const getInitialMetadataState: IMetadataProps[] = [
  {
    id: uniqueId(),
    key: "",
    value: "",
    type: deviceRegistrationTypeOptions[0].value
  }
];

export interface ICreateMetadataProps {
  metadataList?: IMetadataProps[];
  returnMetadataList?: (metadata: IMetadataProps[]) => void;
}

export const CreateMetadata: React.FC<ICreateMetadataProps> = ({
  metadataList: metadataProp,
  returnMetadataList
}) => {
  const [metadataList, setMetadataList] = useState<IMetadataProps[]>(
    metadataProp || getInitialMetadataState
  );

  useEffect(() => {
    returnMetadataList && returnMetadataList(metadataList);
  }, [metadataList]);

  const handleAddParentRow = () => {
    setMetadataList([
      ...metadataList,
      {
        id: uniqueId(),
        key: "",
        value: "",
        type: deviceRegistrationTypeOptions[0].value
      }
    ]);
  };
  let metadataArr: IMetadataProps[] = [];

  const getMetadataRows = (metaDataRows: IMetadataProps[]) => {
    metaDataRows.forEach(metadataRow => {
      metadataArr.push(metadataRow);

      if (
        metadataRow.type === "object" &&
        typeof metadataRow.value == "object" &&
        metadataRow.value.length > 0
      ) {
        getMetadataRows(metadataRow.value);
      }
    });
    return metadataArr.map((metadata, index: number) => {
      return (
        <MetaDataRow
          key={index}
          metadataRow={metadata}
          metadataList={metadataList}
          setMetadataList={setMetadataList}
          rowId={metadata.id}
        />
      );
    });
  };

  return (
    <Grid>
      <GridItem span={12}>
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
              Add another parameter
            </Button>
          </GridItem>
        </Grid>
      </GridItem>
    </Grid>
  );
};
