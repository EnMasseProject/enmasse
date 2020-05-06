/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import {
  Button,
  SelectOptionObject,
  GridItem,
  Grid
} from "@patternfly/react-core";
import { PlusCircleIcon } from "@patternfly/react-icons";
import { MetaDataHeader } from "./MetaDataHeader";
import { MetaDataRow } from "./MetaDataRow";

export interface IMetaData {
  onChangePropertyInput?: (value: string) => Promise<any>;
}

export const MetaData: React.FC<IMetaData> = ({ onChangePropertyInput }) => {
  const [formData, setFormData] = useState([{ defaults: {}, ext: {} }]);

  const handleAddParentRow = () => {
    setFormData([...formData, { defaults: {}, ext: {} }]);
  };

  return (
    <>
      <MetaDataHeader sectionName="Default properties" />
      {formData.map((data, index) => (
        <MetaDataRow
          key={index}
          onChangePropertyInput={onChangePropertyInput}
          formData={formData}
          setFormData={setFormData}
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
    </>
  );
};
