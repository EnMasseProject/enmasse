/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { isMessagingProjectValid } from "modules/msg-and-iot/dailogs";
import { dnsSubDomainRfc1123NameRegexp } from "utils";
import { Configuration } from "modules/address-space";

export interface IMessagingProjectInput {
  messagingProjectName?: string;
  messagingProjectType?: string;
  messagingProjectPlan?: string;
  namespace?: string;
  authenticationService?: string;
  isNameValid: boolean;
}

const MessagingConfigurationStep = (
  setProjectDetail: (value: IMessagingProjectInput) => void,
  projectDetail: IMessagingProjectInput
) => {
  const isReviewEnabled = () => isMessagingProjectValid(projectDetail);
  const setter = (type: string, value?: string) => {
    let newProject: IMessagingProjectInput = Object.assign({}, projectDetail);
    switch (type.toLowerCase()) {
      case "name":
        newProject.messagingProjectName = value;
        if (value) {
          dnsSubDomainRfc1123NameRegexp.test(value)
            ? (newProject.isNameValid = true)
            : (newProject.isNameValid = false);
        }
        break;
      case "namespace":
        newProject.namespace = value;
        break;
      case "type":
        newProject.messagingProjectType = value;
        newProject.messagingProjectPlan = undefined;
        newProject.authenticationService = undefined;
        break;
      case "plan":
        newProject.messagingProjectPlan = value;
        break;
      case "authenticationservice":
        newProject.authenticationService = value;
        break;
    }
    setProjectDetail(newProject);
  };
  const setName = (value: string) => {
    setter("name", value.trim());
  };
  const setNamespace = (value: string) => {
    setter("namespace", value.trim());
  };
  const setType = (value: string) => {
    setter("type", value.trim());
  };
  const setPlan = (value: string) => {
    setter("plan", value.trim());
  };
  const setAuthenticationService = (value: string) => {
    setter("authenticationService", value);
  };
  const setIsNameValid = (value: boolean) => {
    // TODO: remove this function
  };
  const steps = {
    name: "Configuration",
    component: (
      <Configuration
        name={projectDetail.messagingProjectName || ""}
        setName={setName}
        namespace={projectDetail.namespace || ""}
        setNamespace={setNamespace}
        type={projectDetail.messagingProjectType || ""}
        setType={setType}
        plan={projectDetail.messagingProjectPlan || ""}
        setPlan={setPlan}
        authenticationService={projectDetail.authenticationService || ""}
        setAuthenticationService={setAuthenticationService}
        isNameValid={projectDetail.isNameValid}
        setIsNameValid={setIsNameValid}
      />
    ),
    enableNext: isReviewEnabled(),
    backButton: "hide"
  };
  return steps;
};

export { MessagingConfigurationStep };
