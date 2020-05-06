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

  const handleAddParentClick = () => {
    setFormData([...formData, { defaults: {}, ext: {} }]);
  };

  return (
    <>
      <MetaDataHeader sectionName="Default properties" />
      {formData.map((data, index) => {
        return (
          <MetaDataRow
            onChangePropertyInput={onChangePropertyInput}
            formData={formData}
            setFormData={setFormData}
          />
        );
      })}
      <Grid>
        <GridItem span={3}>
          <Button
            variant="link"
            icon={<PlusCircleIcon />}
            onClick={handleAddParentClick}
          >
            Add More
          </Button>
        </GridItem>
      </Grid>
    </>
  );
};
