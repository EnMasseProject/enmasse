/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { isIoTProjectValid } from "modules/msg-and-iot/dailogs/utils";
import { dnsSubDomainRfc1123NameRegexp } from "utils";
import { IDropdownOption } from "components";
import { IoTConfiguration } from "../IoTConfiguration";

export interface IIoTProjectInput {
  iotProjectName?: string;
  namespace?: string;
  isEnabled?: boolean;
  isNameValid?: boolean;
}

const IoTProjectConfiguration = (
  setProjectDetail: (value: IIoTProjectInput) => void,
  namespaces: IDropdownOption[],
  projectDetail?: IIoTProjectInput
) => {
  const [iotProjectName, setIotProjectName] = useState<string>(
    (projectDetail && projectDetail.iotProjectName) || ""
  );
  const [iotProjectNamespace, setIotProjectNamespace] = useState<string>(
    (projectDetail && projectDetail.namespace) || ""
  );
  const [isIotEnabled, setIsIotEnabled] = useState<boolean>(true);
  const [isNameValid, setIsNameValid] = useState<boolean>(true);
  const isReviewEnabled = () => {
    isIoTProjectValid(projectDetail);
  };
  const setData = (value?: string, type?: string, booleanValue?: boolean) => {
    let projectCopy: IIoTProjectInput = projectDetail || {};
    projectCopy.iotProjectName = iotProjectName;
    projectCopy.namespace = iotProjectNamespace;
    projectCopy.isEnabled = isIotEnabled;
    projectCopy.isNameValid = isNameValid;
    switch (type && type.toLowerCase()) {
      case "name":
        projectCopy.iotProjectName = value;
        dnsSubDomainRfc1123NameRegexp.test(value || "")
          ? (projectCopy.isNameValid = false)
          : (projectCopy.isNameValid = true);
        break;
      case "namespace":
        projectCopy.namespace = value;
        break;
      case "enabled":
        projectCopy.isEnabled = booleanValue;
        break;
    }
    setProjectDetail(projectCopy);
  };
  const handleName = (value: string) => {
    setIotProjectName(value.trim());
    !dnsSubDomainRfc1123NameRegexp.test(value.trim())
      ? setIsNameValid(false)
      : setIsNameValid(true);
    setData(value.trim(), "name");
  };
  const handleNamespace = (value: string) => {
    setIotProjectNamespace(value.trim());
    setData(value.trim(), "namespace");
  };
  const setEnabled = (value: boolean) => {
    setIsIotEnabled(value);
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
        namespace={iotProjectNamespace}
        name={iotProjectName}
        isNameValid={isNameValid}
        isEnabled={isIotEnabled}
      />
    ),
    enableNext: isReviewEnabled(),
    backButton: "hide"
  };
  return steps;
};

export { IoTProjectConfiguration };
