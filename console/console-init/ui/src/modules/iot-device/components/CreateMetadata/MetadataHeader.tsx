/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Grid, GridItem, Title, Tooltip } from "@patternfly/react-core";
import { OutlinedQuestionCircleIcon } from "@patternfly/react-icons";

export interface IMetadataHeader {
  sectionName: string;
}

export const MetadataHeader: React.FC<IMetadataHeader> = ({ sectionName }) => {
  return (
    <Grid hasGutter>
      <GridItem span={5}>
        <Grid>
          <GridItem span={10}>
            <Title headingLevel="h6" size="md">
              {sectionName}
            </Title>
          </GridItem>
          <GridItem span={2}>
            <Tooltip
              content={
                <div>
                  Lorem ipsum dolor sit amet, consectetur adipiscing elit.
                  Nullam id feugiat augue, nec fringilla turpis.
                </div>
              }
            >
              <OutlinedQuestionCircleIcon />
            </Tooltip>
          </GridItem>
        </Grid>
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
