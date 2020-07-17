/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import { DeviceFilter, getInitialFilter } from "modules/iot-device";
import {
  Grid,
  GridItem,
  Page,
  PageSectionVariants,
  PageSection
} from "@patternfly/react-core";

export default {
  title: "Device List"
};

export const deviceFilter = () => {
  return (
    <MemoryRouter>
      <Page>
        <PageSection variant={PageSectionVariants.light}>
          <Grid>
            <GridItem span={3}>
              <DeviceFilter filter={getInitialFilter()} setFilter={() => {}} />
            </GridItem>
          </Grid>
        </PageSection>
      </Page>
    </MemoryRouter>
  );
};
