/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";

import { text, number, select } from "@storybook/addon-knobs";
import { action } from "@storybook/addon-actions";
import { ProjectHeaderCard, IProjectCount } from "components";
import { Page } from "@patternfly/react-core";

export default {
  title: "All Project Header"
};

export const AllProjectsWith = () => {
  const ioTCount: IProjectCount = {
    total: 13,
    failed: 0,
    active: 0,
    pending: 0,
    configuring: 0
  };
  const msgCount: IProjectCount = {
    total: 12,
    failed: 1,
    active: 8,
    pending: 2,
    configuring: 1
  };
  return (
    <MemoryRouter>
      <Page>
        <ProjectHeaderCard
          totalProject={number("totalCount", 25)}
          ioTCount={ioTCount}
          msgCount={msgCount}
        />
      </Page>
    </MemoryRouter>
  );
};
