/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { IMessagingProject } from "modules/address-space/dialogs";
import { TlsCertificateType } from "./constant";

const getDetailForDeleteDialog = (selectedItems: any[]) => {
  const detail =
    selectedItems.length > 1
      ? `Are you sure you want to delete all of these address spaces: ${selectedItems.map(
          as => " " + as.name
        )} ?`
      : `Are you sure you want to delete this address space: ${selectedItems[0].name} ?`;
  return detail;
};

const getHeaderForDeleteDialog = (selectedItems: any[]) => {
  const header =
    selectedItems.length > 1
      ? "Delete these Address Spaces ?"
      : "Delete this Address Space ?";
  return header;
};

const isMessagingProjectConfigurationValid = (
  messagingProject: IMessagingProject
) => {
  if (messagingProject) {
    const { name, namespace, type, plan, authService } = messagingProject;
    if (
      name &&
      name.trim() !== "" &&
      namespace &&
      namespace.trim() !== "" &&
      type &&
      type.trim() !== "" &&
      plan &&
      plan.trim() !== "" &&
      authService &&
      authService.trim() !== ""
    ) {
      return true;
    }
    return false;
  }
};

const isEnabledCertificateStep = (messagingProject: IMessagingProject) => {
  if (
    messagingProject.customizeEndpoint === true &&
    (messagingProject.tlsCertificate !== TlsCertificateType.UPLOAD_CERT ||
      (messagingProject.privateKey &&
        messagingProject.privateKey.trim() !== "" &&
        messagingProject.certValue &&
        messagingProject.certValue?.trim() !== ""))
  ) {
    return true;
  }
  return false;
};

const isMessagingProjectValid = (messagingProject: IMessagingProject) => {
  if (
    isMessagingProjectConfigurationValid(messagingProject) &&
    (messagingProject.customizeEndpoint === false ||
      (messagingProject.protocols &&
        messagingProject.protocols.length > 0 &&
        messagingProject.tlsCertificate &&
        messagingProject.tlsCertificate.trim() !== ""))
  ) {
    return true;
  }
  return false;
};

const isRouteStepValid = (messagingProject: IMessagingProject) => {
  const { routesConf } = messagingProject;
  if (routesConf && routesConf.length > 0) {
    let isValid = true;
    routesConf.forEach(route => {
      if (!route.tlsTermination || route.tlsTermination.trim() === "") {
        isValid = false;
      }
    });
    return isValid;
  } else {
    return false;
  }
};

export {
  getDetailForDeleteDialog,
  getHeaderForDeleteDialog,
  isMessagingProjectValid,
  isMessagingProjectConfigurationValid,
  isEnabledCertificateStep,
  isRouteStepValid
};
