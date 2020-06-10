/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import {
  IProject,
  StatusTypes,
  ProjectList,
  ProjectTypes
} from "modules/project";
import { action } from "@storybook/addon-actions";
import { text } from "@storybook/addon-knobs";

export default {
  title: "Project List"
};
const projects: IProject[] = [
  {
    projectType: ProjectTypes.MESSAGING,
    name: "namespace_test1.new_space",
    displayName: "new_space",
    namespace: "namespace_test1",
    plan: "Standard",
    status: StatusTypes.FAILED,
    creationTimestamp: "2020-01-20T11:44:28.607Z",
    errorMessageRate: 3,
    addressCount: 15,
    connectionCount: 3
  },
  {
    projectType: ProjectTypes.IOT,
    name: "devops_jbosstest1.k8s_iot",
    displayName: "k8s_iot",
    namespace: "devops_jbosstest1",
    status: StatusTypes.ACTIVE,
    creationTimestamp: "2020-05-21T14:40:28.607Z",
    errorMessageRate: 25,
    deviceCount: 10500,
    activeCount: 7100
  },
  {
    projectType: ProjectTypes.MESSAGING,
    name: "namespace_test1.new_space",
    displayName: "new_space",
    namespace: "namespace_test1",
    plan: "Brokered",
    status: StatusTypes.PENDING,
    creationTimestamp: "2020-01-20T05:44:28.607Z",
    addressCount: 27,
    connectionCount: 3
  },
  {
    projectType: ProjectTypes.IOT,
    name: "namespace_test1.k8s_iot",
    displayName: "k8s_iot",
    namespace: "namespace_test1",
    status: StatusTypes.CONFIGURING,
    creationTimestamp: "2020-01-20T05:44:28.607Z",
    addressCount: 27,
    connectionCount: 3,
    errorMessages: [text("error message", "pod is not ready.")]
  },
  {
    projectType: ProjectTypes.MESSAGING,
    name: "namespace_test1.new_space",
    displayName: "new_space",
    namespace: "namespace_test1",
    plan: "Brokered",
    status: StatusTypes.FAILED,
    creationTimestamp: "2020-05-21T08:44:28.607Z",
    addressCount: 27,
    connectionCount: 3,
    errorMessageRate: 98,
    errorMessages: [text("error message", "issue with operator")]
  }
];

export const deviceTable = () => (
  <MemoryRouter>
    <ProjectList
      projects={projects}
      onSort={action("sort column")}
      onEdit={action("on Edit projects")}
      onDelete={action("on Delete projects")}
      onEnable={action("on Enable projects")}
      onDisable={action("on DisbonDisable projects")}
      onDownload={action("on Download certificate projects")}
      onSelectProject={action("on Select poject")}
    />
  </MemoryRouter>
);
