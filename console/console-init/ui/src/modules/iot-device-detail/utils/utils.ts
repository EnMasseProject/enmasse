/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

enum DeviceActionType {
  EDIT_METADATA = "EDIT_METADATA",
  EDIT_DEVICE_IN_JSON = "EDIT_DEVICE_IN_JSON",
  CLONE_DEVICE = "CLONE_DEVICE",
  ADD_GATEWAYS = "ADD_GATEWAYS",
  EDIT_GATEWAYS = "EDIT_GATEWAYS",
  ADD_CREDENTIALS = "ADD_CREDENTIALS",
  EDIT_CREDENTIALS = "EDIT_CREDENTIALS",
  CHANGE_CONNECTION_TYPE = "CHANGE_CONNECTION_TYPE"
}

/**
 * configuration-info
 */
const getCredentialFilterType = (
  filterType: string,
  filterValue: string | boolean
) => {
  let type: string = "";
  if (filterType === "enabled") {
    type = filterType;
  } else if (
    filterType !== "all" &&
    filterType !== "enabled" &&
    (!filterValue || filterValue === "all")
  ) {
    type = "type";
  } else if (filterType !== "all" && filterType !== "enabled") {
    type = "auth-id";
  }
  return type;
};

const getCredentialFilterValue = (
  filterType: string,
  filterValue: string | boolean
) => {
  let value: string | boolean = filterValue;
  if (filterType === "enabled") {
    value = true;
  } else if (
    filterType !== "all" &&
    filterType !== "enabled" &&
    (!filterValue || filterValue === "all")
  ) {
    value = filterType;
  }
  return value;
};

export { DeviceActionType, getCredentialFilterType, getCredentialFilterValue };
