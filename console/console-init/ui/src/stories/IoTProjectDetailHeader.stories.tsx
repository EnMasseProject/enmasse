/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import { text, boolean } from "@storybook/addon-knobs";
import { action } from "@storybook/addon-actions";
import { IoTProjectDetailHeader } from "modules/iot-project-detail/components";

export default {
  title: "IoT Project Detail"
};

export const ProjectDetailHeader = () => (
  <MemoryRouter>
    <IoTProjectDetailHeader
      projectName={text("Project Name", "Project name")}
      timeCreated={text("Creation Time", "2019-11-10T05:08:31.489Z")}
      status={text("Status", "Ready")}
      isEnabled={boolean("Enabled", true)}
      changeStatus={action("onEnableChange Clicked")}
      onDelete={action("onDelete Clicked")}
    />
  </MemoryRouter>
);
