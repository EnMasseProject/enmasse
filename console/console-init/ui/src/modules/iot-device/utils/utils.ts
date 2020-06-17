/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

export const getHeaderForDialog = (devices: any[], dialogType: string) => {
  return devices && devices.length > 1
    ? `${dialogType} these Devices ?`
    : `${dialogType} this Device ?`;
};

export const getDetailForDialog = (devices: any[], dialogType: string) => {
  return devices && devices.length > 1
    ? `Are you sure you want to ${dialogType.toLowerCase()} all of these devices: ${devices.map(
        device => device.deviceId + " "
      )} ?`
    : `Are you sure you want to ${dialogType.toLowerCase()} this device: ${
        devices[0].deviceId
      } ?`;
};
