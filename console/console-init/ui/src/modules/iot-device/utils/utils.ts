/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import { convertMetadataOptionsToJson } from "utils";
import { ICredential } from "modules/iot-device/components";

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
    if ("ext" in cred) {
      (newCredentials[index]["ext"] as any) = convertMetadataOptionsToJson(
        newCredentials?.[index]?.ext
      );
    }
  });
  newCredentials = newCredentials && JSON.stringify(newCredentials);
  return newCredentials;
};

export { getHeaderForDialog, getDetailForDialog, serializeCredentials };
