/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import { DeviceFilter } from "modules/device";
import { text } from "@storybook/addon-knobs";
import { action } from "@storybook/addon-actions";
import { Grid, GridItem } from "@patternfly/react-core";

export default {
  title: "Device List"
};

export const deviceFilter = () => {
  return (
    <MemoryRouter>
      <Grid>
        <GridItem span={3}>
          <DeviceFilter />
        </GridItem>
      </Grid>
    </MemoryRouter>
  );
};
