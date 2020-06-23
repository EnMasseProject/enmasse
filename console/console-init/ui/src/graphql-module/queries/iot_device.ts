/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { IDeviceFilter, getInitialFilter } from "modules/iot-device";
import { ISortBy } from "@patternfly/react-table";

const RETURN_IOT_DEVICE_DETAIL = (iotproject: string, deviceId: string) => {
  const IOT_DEVICE_DETAIL = gql(
    `query iot_device_detail{
         devices(
             iotproject:"${iotproject}"
             filter: "\`$.deviceId\` = '${deviceId}'"){
            total
            devices {
                deviceId
                 enabled
                viaGateway
                jsonData  
                credentials   
              }   
          }
     }`
  );
  return IOT_DEVICE_DETAIL;
};

const RETURN_IOT_CREDENTIALS = (iotproject: string, deviceId: string) => {
  const IOT_CREDENTIALS = gql(
    `query iot_credentials{
      credentials(
        iotproject:"${iotproject}"
        deviceId: "${deviceId}"
      ) {
        total   
         credentials
        }
    }`
  );
  return IOT_CREDENTIALS;
};

const DELETE_IOT_DEVICE = gql(
  `mutation delete_iot_device($iotproject: String!, $deviceId: String!) {
    deleteIotDevice(iotproject: $iotproject, deviceId: $deviceId)
  }`
);

const DELETE_CREDENTIALS_FOR_IOT_DEVICE = gql(
  `mutation delete_credentials_for_device(
    $iotproject: String!
    $deviceId: String!
  ) {
    deleteCredentialsForDevice(iotproject: $iotproject, deviceId: $deviceId)
  }`
);

const SORT_RETURN_ALL_DEVICES_FOR_IOT_PROJECT = (sortBy?: ISortBy) => {
  let orderBy = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 1:
        orderBy = "`$.deviceId` ";
        break;
      case 2:
        orderBy = "`$.viaGateway` ";
        break;
      case 3:
        orderBy = "`$.enabled` ";
        break;
      default:
        break;
    }
    if (orderBy !== "" && sortBy.direction) {
      orderBy += sortBy.direction;
    }
  }
  return orderBy;
};

const FILTER_RETURN_ALL_DEVICES_FOR_IOT_PROJECT = (
  filterObject: IDeviceFilter
) => {
  const { deviceId } = filterObject;
  let filter: string = "";

  if (deviceId && deviceId.trim() !== "") {
    filter += "`$.deviceId` = '" + deviceId + "'";
  }

  // TODO: Needs to handle more parameters once the mock supports it.

  return filter;
};

const RETURN_ALL_DEVICES_FOR_IOT_PROJECT = (
  iotproject: string,
  sortBy?: ISortBy,
  filterObj?: IDeviceFilter,
  queryResolver?: string
) => {
  const defaultQueryResolver = `
    total
    devices {
      deviceId
      jsonData
      enabled
      viaGateway
    }
  `;

  if (!queryResolver) {
    queryResolver = defaultQueryResolver;
  }

  let filter = FILTER_RETURN_ALL_DEVICES_FOR_IOT_PROJECT(
    filterObj || getInitialFilter()
  );

  let orderBy = SORT_RETURN_ALL_DEVICES_FOR_IOT_PROJECT(sortBy);

  const ALL_DEVICE_LIST = gql(
    `query devices_for_iot_project {
      devices(iotproject: "${iotproject}", orderBy:"${orderBy}", filter: "${filter}") {
        ${queryResolver}
      }
    }`
  );

  return ALL_DEVICE_LIST;
};

const UPDATE_IOT_DEVICE = gql(
  `mutation update_iot_device(
    $project: String!
    $device: Device_iot_console_input!
  ) {
    updateIotDevice(iotproject: $project, device: $device) {
      deviceId
    }
  }`
);

export {
  RETURN_IOT_DEVICE_DETAIL,
  RETURN_IOT_CREDENTIALS,
  DELETE_IOT_DEVICE,
  UPDATE_IOT_DEVICE,
  RETURN_ALL_DEVICES_FOR_IOT_PROJECT,
  DELETE_CREDENTIALS_FOR_IOT_DEVICE
};
