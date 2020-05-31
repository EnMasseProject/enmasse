/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { IMessagingProjectInput, IIoTProjectInput } from "modules/project";
import { IProjectFilter } from "../ProjectPage";

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

export {
  isMessagingProjectValid,
  isIoTProjectValid,
  initialiseFilterForProject
};
