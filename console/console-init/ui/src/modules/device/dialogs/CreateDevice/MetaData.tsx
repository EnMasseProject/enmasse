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
  onPropertySelect: (e: any, selection: SelectOptionObject) => void;
  onChangePropertyInput?: (value: string) => Promise<any>;
  onPropertyClear: () => void;
  propertySelected?: string;
  propertyInput?: string;
  setPropertyInput?: (value: string) => void;
}

export const MetaData: React.FC<IMetaData> = ({
  onPropertySelect,
  onChangePropertyInput,
  onPropertyClear,
  propertySelected,
  propertyInput,
  setPropertyInput
}) => {
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
            onPropertyClear={onPropertyClear}
            onPropertySelect={onPropertySelect}
            onChangePropertyInput={onChangePropertyInput}
            propertyInput={propertyInput}
            propertySelected={propertySelected}
            setPropertyInput={setPropertyInput}
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
