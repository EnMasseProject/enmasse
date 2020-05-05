import React from "react";
import { Grid, GridItem } from "@patternfly/react-core";

export interface IMetaDataHeader {
  sectionName: string;
}

export const MetaDataHeader: React.FC<IMetaDataHeader> = sectionName => {
  return (
    <Grid gutter="sm">
      <GridItem span={5}>
        {/* <b>{sectionName} parameter</b> */}
        <b>Default properties parameter</b>
      </GridItem>
      <GridItem span={2}>
        <b>Type</b>
      </GridItem>
      <GridItem span={5}>
        <b>Value</b>
      </GridItem>
    </Grid>
  );
};
