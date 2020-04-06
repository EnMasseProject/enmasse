/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { Configuration } from "../../../../address-space/dialogs/CreateAddressSpace/Configuration";
import { isMessagingProjectValid } from "modules/msg-and-iot/dailogs/utils";
import { type } from "os";

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

  const isReviewEnabled = () => isMessagingProjectValid(projectDetail);

  const setter = (value: string, type?: string) => {
    let projectCopy: IMessagingProjectInput = projectDetail || {};
    projectCopy.messagingProjectName = messagingProjectName;
    projectCopy.messagingProjectType = messagingProjectType;
    projectCopy.messagingProjectPlan = messagingProjectPlan;
    projectCopy.namespace = messagingProjectNamespace;
    projectCopy.authenticationService = messagingProjectAuthenticationService;
    projectCopy.isNameValid = isNameValid;
    switch (type) {
      case "name":
        projectCopy.messagingProjectName = value;
        break;
      case "namespace":
        projectCopy.namespace = value;
        break;
      case "type":
        projectCopy.messagingProjectType = value;
        break;
      case "plan":
        projectCopy.messagingProjectPlan = value;
        break;
      case "authenticationService":
        projectCopy.authenticationService = value;
        break;
    }
    setProjectDetail(projectCopy);
  };
  const setName = (value: string) => {
    setMessagingProjectName(value.trim());
    setter(value.trim(), "name");
  };
  const setNamespace = (value: string) => {
    setMessagingProjectNamespace(value.trim());
    setter(value.trim(), "namespace");
  };
  const setType = (value: string) => {
    setMessagingProjectType(value.trim());
    setter(value.trim(), "type");
  };
  const setPlan = (value: string) => {
    setMessagingProjectPlan(value.trim());
    setter(value.trim(), "plan");
  };
  const setAuthenticationService = (value: string) => {
    setMessagingProjectAuthenticationService(value.trim());
    setter(value.trim(), "authenticationService");
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
