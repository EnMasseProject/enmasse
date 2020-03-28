/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Configuration } from "../../../../address-space/dialogs/CreateAddressSpace/Configuration";
import { isMessagingProjectValid } from "modules/msg-and-iot/dailogs/utils";

export interface IMessagingProjectInput {
  messagingProjectName?: string;
  messagingProjectType?: string;
  messagingProjectPlan?: string;
  namespace?: string;
  authenticationService?: string;
  isNameValid?: boolean;
}

const MessagingProjectConfiguration = (
  setProjectDetail: (value: IMessagingProjectInput) => void,
  projectDetail?: IMessagingProjectInput
) => {
  const [messagingProjectName, setMessagingProjectName] = useState(
    (projectDetail && projectDetail.messagingProjectName) || ""
  );
  const [messagingProjectType, setMessagingProjectType] = useState(
    (projectDetail && projectDetail.messagingProjectType) || " "
  );
  const [messagingProjectPlan, setMessagingProjectPlan] = useState(
    (projectDetail && projectDetail.messagingProjectPlan) || " "
  );
  const [messagingProjectNamespace, setMessagingProjectNamespace] = useState(
    (projectDetail && projectDetail.namespace) || " "
  );
  const [
    messagingProjectAuthenticationService,
    setMessagingProjectAuthenticationService
  ] = useState((projectDetail && projectDetail.authenticationService) || " ");
  const [isNameValid, setIsNameValid] = useState(true);

  const isReviewEnabled = () => {
    isMessagingProjectValid(projectDetail);
  };
  const setName = (value: string) => {
    setMessagingProjectName(value.trim());
    let projectCopy: IMessagingProjectInput = projectDetail || {};
    projectCopy.messagingProjectName = value.trim();
    projectCopy.messagingProjectType = messagingProjectType;
    projectCopy.messagingProjectPlan = messagingProjectPlan;
    projectCopy.namespace = messagingProjectNamespace;
    projectCopy.authenticationService = messagingProjectAuthenticationService;
    projectCopy.isNameValid = isNameValid;
    setProjectDetail(projectCopy);
  };
  const setNamespace = (value: string) => {
    setMessagingProjectNamespace(value.trim());
    let projectCopy: IMessagingProjectInput = projectDetail || {};
    projectCopy.messagingProjectName = messagingProjectName;
    projectCopy.messagingProjectType = messagingProjectType;
    projectCopy.messagingProjectPlan = messagingProjectPlan;
    projectCopy.namespace = value.trim();
    projectCopy.authenticationService = messagingProjectAuthenticationService;
    projectCopy.isNameValid = isNameValid;
    setProjectDetail(projectCopy);
  };
  const setType = (value: string) => {
    setMessagingProjectType(value.trim());
    let projectCopy: IMessagingProjectInput = projectDetail || {};
    projectCopy.messagingProjectName = messagingProjectName;
    projectCopy.messagingProjectType = value.trim();
    projectCopy.messagingProjectPlan = messagingProjectPlan;
    projectCopy.namespace = messagingProjectNamespace;
    projectCopy.authenticationService = messagingProjectAuthenticationService;
    projectCopy.isNameValid = isNameValid;
    setProjectDetail(projectCopy);
  };
  const setPlan = (value: string) => {
    setMessagingProjectPlan(value.trim());
    let projectCopy: IMessagingProjectInput = projectDetail || {};
    projectCopy.messagingProjectName = messagingProjectName;
    projectCopy.messagingProjectType = messagingProjectType;
    projectCopy.messagingProjectPlan = value.trim();
    projectCopy.namespace = messagingProjectNamespace;
    projectCopy.authenticationService = messagingProjectAuthenticationService;
    projectCopy.isNameValid = isNameValid;
    setProjectDetail(projectCopy);
  };
  const setAuthenticationService = (value: string) => {
    setMessagingProjectAuthenticationService(value.trim());
    let projectCopy: IMessagingProjectInput = projectDetail || {};
    projectCopy.messagingProjectName = messagingProjectName;
    projectCopy.messagingProjectType = messagingProjectType;
    projectCopy.messagingProjectPlan = messagingProjectPlan;
    projectCopy.namespace = messagingProjectNamespace;
    projectCopy.authenticationService = value.trim();
    projectCopy.isNameValid = isNameValid;
    setProjectDetail(projectCopy);
  };
  const steps = {
    name: "Configuration",
    component: (
      <Configuration
        name={messagingProjectName}
        setName={setName}
        namespace={messagingProjectNamespace}
        setNamespace={setNamespace}
        type={messagingProjectType}
        setType={setType}
        plan={messagingProjectPlan}
        setPlan={setPlan}
        authenticationService={messagingProjectAuthenticationService}
        setAuthenticationService={setAuthenticationService}
        isNameValid={isNameValid}
        setIsNameValid={setIsNameValid}
      />
    ),
    enableNext: isReviewEnabled(),
    backButton: "hide"
  };
  return steps;
};

export { MessagingProjectConfiguration };
