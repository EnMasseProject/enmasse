/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import ReactDom from "react-dom";
import { MemoryRouter } from "react-router";
import { render, cleanup } from "@testing-library/react";
import { StatusTypes, ProjectTypes } from "modules/project/utils";
import { ProjectList, IProject } from "./ProjectList";

afterEach(cleanup);

describe("<ProjectList/>", () => {
  const projects: IProject[] = [
    {
      projectType: ProjectTypes.MESSAGING,
      name: "namespace_test1.new_space",
      displayName: "new_space",
      namespace: "namespace_test1",
      plan: "standard-medium",
      type: "standard",
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
    // for null value
    {
      projectType: ProjectTypes.IOT
    }
  ];
  it("should render without crashing", () => {
    const div = document.createElement("div");
    ReactDom.render(
      <MemoryRouter>
        <ProjectList
          projects={projects}
          onSelectProject={jest.fn()}
          onEdit={jest.fn()}
          onDelete={jest.fn()}
          onDownload={jest.fn()}
          onEnable={jest.fn()}
          onDisable={jest.fn()}
        />
      </MemoryRouter>,
      div
    );
  });
  it("should render a list of address spaces", () => {
    const { getByText } = render(
      <MemoryRouter>
        <ProjectList
          projects={projects}
          onSelectProject={jest.fn()}
          onEdit={jest.fn()}
          onDelete={jest.fn()}
          onDownload={jest.fn()}
          onEnable={jest.fn()}
          onDisable={jest.fn()}
        />
      </MemoryRouter>
    );
    // table Header
    getByText("Name");
    getByText("Type/Plan");
    getByText("Status");
    getByText("Time created");
    getByText("Error messages");
    getByText("Entities");

    const getProjectPlan = () => {
      const project: IProject = projects[0];
      if (project && project.type) {
        return (
          project.type[0].toUpperCase() +
          project.type.substring(1, project.type.length)
        );
      }
    };
    //first row's data
    getByText(projects[0].displayName || "");
    getByText(projects[0].projectType + " " + getProjectPlan());
    //second row's data
    getByText(projects[1].displayName || "");
  });
});
