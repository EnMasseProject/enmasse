/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import {
  convertMetadataOptionsToJson,
  uniqueId,
  convertJsonToMetadataOptions,
  deepClean
} from "utils";
import {
  ICredential,
  ISecret,
  IExtension
} from "modules/iot-device/components";
import { CredentialsType } from "constant";
import { ICreateDeviceRequest } from "schema/iot_device";

const getHeaderForDialog = (devices: any[], dialogType: string) => {
  return devices && devices.length > 1
    ? `${dialogType} these Devices ?`
    : `${dialogType} this Device ?`;
};

const getDetailForDialog = (devices: any[], dialogType: string) => {
  return devices && devices.length > 1
    ? `Are you sure you want to ${dialogType.toLowerCase()} all of these devices: ${devices.map(
        device => device.deviceId + " "
      )} ?`
    : `Are you sure you want to ${dialogType.toLowerCase()} this device: ${
        devices[0].deviceId
      } ?`;
};

const serializeCredentials = (credentials: ICredential[] = []) => {
  let newCredentials: any = [...credentials];
  newCredentials?.map((cred: any, index: number) => {
    deepClean(cred, ["isExpandedAdvancedSetting", "id"]);
    if ("ext" in cred) {
      (newCredentials[index]["ext"] as any) = convertMetadataOptionsToJson(
        newCredentials?.[index]?.ext
      );
    }
  });

  newCredentials = newCredentials && JSON.stringify(newCredentials);
  return newCredentials;
};

const deserializeCredentials = (
  credentials: ICredential[] = [],
  initialSecret?: any
) => {
  const newCredentials = [...credentials];
  newCredentials?.map((cred: ICredential, credIndex: number) => {
    const { secrets } = cred || {};
    newCredentials[credIndex]["id"] = uniqueId();
    /**
     * merge initial secret state with server response
     */
    if (secrets?.length > 0) {
      secrets?.map((secret: any, srtIndex: number) => {
        (newCredentials[credIndex]["secrets"] as any)[srtIndex] = {
          ...initialSecret,
          ...secret
        };
      });
    } else {
      /**
       * set initial secret state in case secrets doesn't available in server response
       */
      let initialStateSecret = {};
      const initialState = getFormInitialStateByProperty(
        newCredentials,
        "secrets",
        credIndex || 0
      );
      initialStateSecret = { id: uniqueId(), ...initialState };
      (newCredentials[credIndex]["secrets"] as any) = [initialStateSecret];
    }
    /**
     * merge initial ext state with server response
     */
    const ext = newCredentials[credIndex]["ext"] || {};
    if (Object.keys(ext)?.length > 0) {
      (newCredentials[credIndex]["ext"] as any) = convertJsonToMetadataOptions(
        ext,
        undefined,
        true
      );
    } else {
      /**
       * set initial ext state in case ext doesn't available in server response
       */
      (newCredentials[credIndex]["ext"] as any) = [
        getExtensionsFieldsInitialState()
      ];
    }
  });
  return newCredentials;
};

const getSecretsFieldsInitialState = (
  credentials: ICredential[],
  credIndex: number = 0
) => {
  let initialState: ISecret = {};
  const type = credentials?.[credIndex]?.type;
  switch (type && type.toLowerCase()) {
    case CredentialsType.PASSWORD:
      initialState = {
        "pwd-hash": "",
        "not-before": "",
        "not-after": "",
        comment: ""
      };
      break;
    case CredentialsType.X509_CERTIFICATE:
      initialState = { "not-before": "", "not-after": "", comment: "" };
      break;
    case CredentialsType.PSK:
      initialState = {
        key: "",
        "not-before": "",
        "not-after": "",
        comment: ""
      };
      break;
    default:
      break;
  }
  return initialState;
};

const getExtensionsFieldsInitialState = () => {
  const initialState: IExtension = {
    id: uniqueId(),
    key: "",
    type: "",
    value: ""
  };
  return initialState;
};

const getCredentialsFieldsInitialState = () => {
  const initialState: ICredential = {
    id: uniqueId(),
    "auth-id": "",
    type: "hashed-password",
    secrets: [
      {
        id: uniqueId(),
        "pwd-hash": "",
        "not-before": "",
        "not-after": "",
        comment: ""
      }
    ],
    ext: [getExtensionsFieldsInitialState()],
    enabled: true,
    isExpandedAdvancedSetting: false
  };
  return initialState;
};

const getFormInitialStateByProperty = (
  credentials: ICredential[],
  property: string,
  credIndex: number = 0
) => {
  let initialState = {};
  switch (property?.toLowerCase()) {
    case "credentials":
      initialState = getCredentialsFieldsInitialState();
      break;
    case "secrets":
      initialState = getSecretsFieldsInitialState(credentials, credIndex);
      break;
    case "ext":
      initialState = getExtensionsFieldsInitialState();
      break;
    default:
      break;
  }
  return initialState;
};

const getDeviceFromDeviceString = (device: string) => {
  const deviceDetail: ICreateDeviceRequest = {
    registration: {
      enabled: true
    }
  };
  let parsedDevice: any;
  try {
    parsedDevice = JSON.parse(device);
  } catch (_err) {
    // Inavalid json string
  }
  if (parsedDevice) {
    const { id, registration, credentials } = parsedDevice;
    if (id && id !== "") {
      deviceDetail.deviceId = id;
    }
    if (registration) {
      if (registration.enabled !== undefined) {
        deviceDetail.registration.enabled = registration.enabled;
      }
      if (registration.defaults && registration.defaults.length > 0) {
        try {
          deviceDetail.registration.defaults = JSON.stringify(
            registration.defaults
          );
        } catch (_err) {
          // Inavalid json string
        }
      }
      if (registration.via && registration.via.length > 0) {
        deviceDetail.registration.via = registration.via;
      }
      if (registration.viaGroups && registration.viaGroups.length > 0) {
        deviceDetail.registration.viaGroups = registration.viaGroups;
      }
      if (registration.memberOf && registration.memberOf.length > 0) {
        deviceDetail.registration.memberOf = registration.memberOf;
      }

      if (registration.ext && registration.ext.length > 0) {
        try {
          deviceDetail.registration.ext = JSON.stringify(registration.ext);
        } catch (_err) {
          // Inavalid json string
        }
      }
    }
    if (credentials && credentials.length > 0) {
      try {
        deviceDetail.credentials = JSON.stringify(credentials);
      } catch (_err) {
        // Inavalid json string
      }
    }
  }
  return deviceDetail;
};

export {
  getHeaderForDialog,
  getDetailForDialog,
  serializeCredentials,
  getFormInitialStateByProperty,
  getCredentialsFieldsInitialState,
  getExtensionsFieldsInitialState,
  getSecretsFieldsInitialState,
  deserializeCredentials,
  getDeviceFromDeviceString
};
