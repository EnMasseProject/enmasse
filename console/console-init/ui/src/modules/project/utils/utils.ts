/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import {
  IMessagingProjectInput,
  IIoTProjectInput,
  IProjectCount,
  IProject
} from "modules/project";
import { IProjectFilter } from "modules/project/ProjectPage";
import { ISelectOption } from "utils";
import { ProjectTypes, StatusTypes } from "./constant";

enum ProjectType {
  IOT_PROJECT = "IoT",
  MESSAGING_PROJECT = "Messaging"
}

const sortMenuItems = [
  { key: "name", value: "Name", index: 1 },
  { key: "type/plan", value: "Type/Plan", index: 2 },
  { key: "status", value: "Status", index: 3 },
  { key: "creationTimestamp", value: "Time Created", index: 4 }
];

const filterMenuItems = [
  { key: "name", value: "Name", id: "dropdown-filter-name" },
  { key: "namespace", value: "Namespace", id: "dropdown-filter-namespace" },
  { key: "type", value: "Type", id: "dropdown-filter-type" }
];

const typeOptions: ISelectOption[] = [
  { key: "iot", value: "IoT Project", isDisabled: false },
  {
    key: "standard",
    value: "Messaging Project - Standard",
    isDisabled: false
  },
  {
    key: "brokered",
    value: "Messaging Project - Brokered Messaging",
    isDisabled: false
  }
];

const isMessagingProjectValid = (projectDetail?: IMessagingProjectInput) => {
  if (
    projectDetail &&
    projectDetail.authenticationService &&
    projectDetail.authenticationService.trim() !== "" &&
    projectDetail.messagingProjectName &&
    projectDetail.messagingProjectName.trim() !== "" &&
    projectDetail.messagingProjectPlan &&
    projectDetail.messagingProjectPlan.trim() !== "" &&
    projectDetail.messagingProjectType &&
    projectDetail.messagingProjectType.trim() !== "" &&
    projectDetail.namespace &&
    projectDetail.namespace.trim() !== "" &&
    projectDetail.isNameValid
  ) {
    return true;
  }
  return false;
};

const isIoTProjectValid = (projectDetail?: IIoTProjectInput) => {
  if (
    projectDetail &&
    projectDetail.iotProjectName &&
    projectDetail.iotProjectName.trim() !== "" &&
    projectDetail.namespace &&
    projectDetail.namespace.trim() !== "" &&
    projectDetail.isNameValid
  ) {
    return true;
  }
  return false;
};

const initialiseFilterForProject = () => {
  const filter: IProjectFilter = {
    filterType: "Name",
    names: [],
    namespaces: []
  };
  return filter;
};

const setInitialProjcetCount = () => {
  const count: IProjectCount = {
    total: 0,
    active: 0,
    pending: 0,
    configuring: 0,
    failed: 0
  };
  return count;
};
const getFilteredProjectsCount = (
  type: ProjectTypes,
  projectList: IProject[],
  status?: StatusTypes
) => {
  let list: IProject[] = [];
  if (!status) {
    list = projectList.filter(project => project.projectType === type);
  } else {
    list = projectList.filter(
      project => project.projectType === type && project.status === status
    );
  }
  console.log(type, list);
  return list.length;
};

export {
  isMessagingProjectValid,
  isIoTProjectValid,
  initialiseFilterForProject,
  sortMenuItems,
  typeOptions,
  filterMenuItems,
  ProjectType,
  setInitialProjcetCount,
  getFilteredProjectsCount
};
