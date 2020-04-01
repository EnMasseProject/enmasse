import { IMessagingProjectInput } from "../components/MessagingProjectConfiguration";

/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

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
  } else {
    return false;
  }
};

export { isMessagingProjectValid };
