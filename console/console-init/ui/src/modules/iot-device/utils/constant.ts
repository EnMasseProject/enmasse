/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { ISelectOption } from "utils";
import { IDeviceFilter } from "modules/iot-device/components";
import { AlertVariant } from "@patternfly/react-core";

/**
 * dropdown options
 */
const deviceTypeOptions: ISelectOption[] = [
  {
    key: "direct",
    value: "direct",
    label: "Directly connected"
  },
  {
    key: "gateway",
    value: "gateway",
    label: "Using gateways"
  },
  {
    key: "alltypes",
    value: "allTypes",
    label: "All types"
  }
];
const deviceStatusOptions: ISelectOption[] = [
  {
    key: "enabled",
    value: "enabled",
    label: "Enabled"
  },
  {
    key: "disabled",
    value: "disabled",
    label: "Disabled"
  },
  {
    key: "allstatus",
    value: "allStatus",
    label: "All status"
  }
];

const credentialTypeOptions: ISelectOption[] = [
  { key: "hashed-password", label: "Password", value: "hashed-password" },
  { key: "x509-cert", label: "X.509 Certificate", value: "x509-cert" },
  { key: "psk", label: "PSK", value: "psk" }
];

const getInitialFilter = () => {
  let filter: IDeviceFilter = {
    deviceId: "",
    deviceType: "allTypes",
    status: "allStatus",
    filterCriteria: [],
    addedDate: {
      startDate: "",
      endDate: ""
    },
    lastSeen: {
      startTime: {
        form: "hr",
        time: ""
      },
      endTime: {
        form: "hr",
        time: ""
      }
    }
  };
  return filter;
};

const getInitialAlert = () => {
  const alert = {
    isVisible: false,
    variant: AlertVariant.default,
    title: "",
    description: ""
  };
  return alert;
};

const getInitialSelectedColumns = () => {
  return [
    "deviceId",
    "connectionType",
    "status",
    "lastUpdated",
    "lastSeen",
    "addedDate"
  ];
};

const getInitialManageColumnsForDevices = () => {
  return [
    { key: "deviceid", value: "deviceId", label: "Device ID", isChecked: true },
    {
      key: "connection-type",
      value: "connectionType",
      label: "Connection Type",
      isChecked: true
    },
    { key: "status", value: "status", label: "Status", isChecked: true },
    {
      key: "last-updated",
      value: "lastUpdated",
      label: "Last updated",
      isChecked: true
    },
    {
      key: "last-seen",
      value: "lastSeen",
      label: "Last Seen",
      isChecked: true
    },
    {
      key: "added-date",
      value: "addedDate",
      label: "Added Date",
      isChecked: true
    },
    {
      key: "memberof",
      value: "memberOf",
      label: "Member Of",
      isChecked: false
    },
    {
      key: "via-gateways",
      value: "viaGateways",
      label: "Via Gateways",
      isChecked: false
    }
  ];
};
/**
 * key value constants
 */
const HIDE_ADVANCE_SETTING = "Hide advance setting";
const SHOW_ADVANCE_SETTING = "Show advanced setting";

const deviceRegistrationTypeOptions: ISelectOption[] = [
  { key: "string", label: "String", value: "string" },
  { key: "number", label: "Numeric", value: "number" },
  { key: "boolean", label: "Boolean", value: "boolean" },
  { key: "object", label: "Object", value: "object" },
  { key: "array", label: "Array", value: "array" }
];

const getInitialMetadataState = () => {
  const initialState = {
    key: "",
    value: [],
    type: deviceRegistrationTypeOptions[0].value
  };
  return initialState;
};

const MAX_DEVICE_LIST_COUNT = 500;

export {
  deviceTypeOptions,
  deviceStatusOptions,
  getInitialFilter,
  getInitialAlert,
  HIDE_ADVANCE_SETTING,
  SHOW_ADVANCE_SETTING,
  credentialTypeOptions,
  deviceRegistrationTypeOptions,
  getInitialMetadataState,
  getInitialSelectedColumns,
  getInitialManageColumnsForDevices,
  MAX_DEVICE_LIST_COUNT
};
