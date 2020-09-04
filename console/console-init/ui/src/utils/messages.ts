/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

const getTitleForSuccessQuery = (action: string) => {
  /**
   * Todo: add title for all actions
   */
  const titles: any = {
    createAddressSpace: "Address space created",
    createAddress: "Address created",
    createIotDevice: "Device created",
    createIotProject: "Project created",
    updateIotDevice: "Device setting updated",
    deleteAddressSpace: "Address space deleted",
    deleteAddressSpaces: "Selected address spaces deleted",
    deleteAddress: "Address deleted",
    deleteAddresses: "Selected addresses deleted",
    deleteIotDevices: "Selected devices deleted",
    deleteCredentialsForDevice: "Selected connection information removed.",
    deleteIotProjects: "Selected projects deleted",
    patchAddressSpace: "Address space patched",
    patchAddress: "Address patched",
    patchIotProject: "",
    purgeAddress: "Address purged.",
    purgeAddresses: "Selected addresses purged.",
    closeConnections: "Selected connections closed",
    setCredentialsForDevice: "Credentials setting updated.",
    toggleIoTProjectsStatus: "Selected projects status updated.",
    toggleIoTProjectStatus: "Project status updated.",
    toggleIoTDevicesStatus: "Selected devices status updated.",
    toggleIoTDeviceStatus: "Device status updated."
  };

  if (action in titles) {
    return titles[action];
  }
  return "Action has completed successfuly";
};

const getTitleForFailedQuery = (action: string) => {
  /**
   * Todo: add title for all actions
   */
  const titles: any = {
    createAddressSpace: "Address space creation failed, Please try again.",
    createAddress: "Address creation failed. Please try again.",
    createIotDevice: "Device creation failed. Please try again.",
    createIotProject: "Project creation failed. Please try again.",
    updateIotDevice: "Device setting update failed. Please try again.",
    deleteAddressSpace: "Deleting address space failed. Please try again.",
    deleteAddressSpaces:
      "Deleting selected address spaces failed. Please try again.",
    deleteAddress: "Deleting address failed. Please try again.",
    deleteAddresses: "Deleting selected addresses failed. Please try again.",
    deleteIotDevices: "Deleting device failed. Please try again.",
    deleteCredentialsForDevice:
      " Deleting selected connection information failed. Please try again.",
    deleteIotProjects: "Deleting selected projects failed. Please try again.",
    patchAddressSpace: "Address space patch failed. Please try again.",
    patchAddress: "Address patch failed. Please try again.",
    patchIotProject: "",
    purgeAddress: "Address purge failed. Please try again.",
    purgeAddresses: "Selected addresses purge failed. Please try again.",
    closeConnections: "Closing connections failed. Please try again.",
    setCredentialsForDevice:
      "Credentials setting update failed. Please try again.",
    toggleIoTProjectsStatus:
      "Unable to enable/disable selected projects. Please try again.",
    toggleIoTProjectStatus:
      "Unable to enable/disable project. Please try again.",
    toggleIoTDevicesStatus:
      "Unable to enable/disable selected devices. Please try again.",
    toggleIoTDeviceStatus: "Unable to enable/disable device. Please try again."
  };

  if (action in titles) {
    return titles[action];
  }
  return "Server error";
};

const getLabelByKey = (key: string) => {
  const keyLabels: any = {
    "auth-id": "Auth ID",
    type: "Credential type",
    "not-after": "Not after",
    "not-before": "Not before",
    "pwd-hash": "Password",
    "hashed-password": "Password",
    psk: "PSK",
    "x-509": "X-509 certificate",
    "subject-dn": "Subject-dn",
    "public-key": "Public key",
    "auto-provisioning-enabled": "Auto-provision",
    "x509-cert": "X-509 certificate"
  };

  if (key in keyLabels) {
    return keyLabels[key];
  }
  return key;
};

export { getTitleForSuccessQuery, getTitleForFailedQuery, getLabelByKey };
