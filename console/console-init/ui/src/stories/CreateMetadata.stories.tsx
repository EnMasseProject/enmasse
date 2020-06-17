/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Grid, GridItem } from "@patternfly/react-core";
import { CreateMetadata } from "modules/iot-device/components";

export default {
  title: "Create Metadata"
};

export const CreateMetadataPage = () => (
  <Grid gutter="sm">
    <GridItem span={6}>
      <CreateMetadata />
    </GridItem>
  </Grid>
);
