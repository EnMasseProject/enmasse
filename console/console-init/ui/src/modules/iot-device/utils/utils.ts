/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import _ from "lodash";
import { convertMetadataOptionsToJson, uniqueId } from "utils";
import {
  ICredential,
  ISecret,
  IExtension
} from "modules/iot-device/components";
import { CredentialsType } from "constant";

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
    newCredentials[index] = _.omit(cred, ["isExpandedAdvancedSetting"]);
    if ("ext" in cred) {
      (newCredentials[index]["ext"] as any) = convertMetadataOptionsToJson(
        newCredentials?.[index]?.ext
      );
    }
  });
  newCredentials = newCredentials && JSON.stringify(newCredentials);
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
    secrets: [{ "pwd-hash": "" }],
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

export {
  getHeaderForDialog,
  getDetailForDialog,
  serializeCredentials,
  getFormInitialStateByProperty,
  getCredentialsFieldsInitialState,
  getExtensionsFieldsInitialState,
  getSecretsFieldsInitialState
};
