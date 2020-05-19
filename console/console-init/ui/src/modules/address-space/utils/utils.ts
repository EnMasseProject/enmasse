/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { IMessagingProject } from "../dialogs";

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

const isMessgaingProjectConfigurationValid = (
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
const isMessgaingProjectValid = (messagingProject: IMessagingProject) => {
  if (isMessgaingProjectConfigurationValid(messagingProject)) {
    if (messagingProject.customizeEndpoint) {
      if (
        messagingProject.protocols &&
        messagingProject.protocols.length > 0 &&
        messagingProject.tlsCertificate &&
        messagingProject.tlsCertificate.trim() !== ""
      ) {
        return true;
      }
    } else {
      return true;
    }
  }
  return false;
};
export {
  getDetailForDeleteDialog,
  getHeaderForDeleteDialog,
  isMessgaingProjectValid,
  isMessgaingProjectConfigurationValid
};
