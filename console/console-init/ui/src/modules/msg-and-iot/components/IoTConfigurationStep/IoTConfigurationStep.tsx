/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { IoTConfiguration, isIoTProjectValid } from "modules/msg-and-iot";
import { dnsSubDomainRfc1123NameRegexp } from "utils";
import { IDropdownOption } from "components";

export interface IIoTProjectInput {
  iotProjectName?: string;
  namespace?: string;
  isEnabled: boolean;
  isNameValid: boolean;
}

const IoTConfigurationStep = (
  setProjectDetail: (value: IIoTProjectInput) => void,
  namespaces: IDropdownOption[],
  projectDetail: IIoTProjectInput
) => {
  const isReviewEnabled = () => {
    isIoTProjectValid(projectDetail);
  };
  const setData = (value?: string, type?: string, booleanValue?: boolean) => {
    let newProject: IIoTProjectInput = Object.assign({}, projectDetail);
    switch (type && type.toLowerCase()) {
      case "name":
        newProject.iotProjectName = value;
        if (value) {
          dnsSubDomainRfc1123NameRegexp.test(value)
            ? (newProject.isNameValid = true)
            : (newProject.isNameValid = false);
        }
        break;
      case "namespace":
        newProject.namespace = value;
        break;
      case "enabled":
        if (booleanValue !== undefined) {
          newProject.isEnabled = booleanValue;
        }
        break;
    }
    setProjectDetail(newProject);
  };
  const handleName = (value: string) => {
    setData(value.trim(), "name");
  };
  const handleNamespace = (value: string) => {
    setData(value.trim(), "namespace");
  };
  const setEnabled = (value: boolean) => {
    setData(undefined, "enabled", value);
  };
  const steps = {
    name: "Configuration",
    component: (
      <IoTConfiguration
        onNameSpaceSelect={handleNamespace}
        handleNameChange={handleName}
        handleEnabledChange={setEnabled}
        namespaceOptions={namespaces}
        namespace={projectDetail.namespace || ""}
        name={projectDetail.iotProjectName || ""}
        isNameValid={projectDetail.isNameValid}
        isEnabled={projectDetail.isEnabled}
      />
    ),
    enableNext: isReviewEnabled(),
    backButton: "hide"
  };
  return steps;
};

export { IoTConfigurationStep };
