import { ISelectOption } from "utils";
import { IDeviceFilter } from "../components";

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
  { key: "hashed_password", label: "Password", value: "hashed_password" },
  { key: "x509", label: "X-509 Certificate", value: "x509" },
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

/**
 * key value constants
 */
const HIDE_ADVANCE_SETTING = "Hide advance setting";
const SHOW_ADVANCE_SETTING = "Show advanced setting";

export {
  deviceTypeOptions,
  deviceStatusOptions,
  getInitialFilter,
  HIDE_ADVANCE_SETTING,
  SHOW_ADVANCE_SETTING,
  credentialTypeOptions
};
