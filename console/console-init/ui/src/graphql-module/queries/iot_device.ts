/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { IDeviceFilter, getInitialFilter } from "modules/iot-device";
import { ISortBy } from "@patternfly/react-table";

const RETURN_IOT_DEVICE_DETAIL = (iotproject: string, deviceId: string) => {
  const IOT_DEVICE_DETAIL = gql`
     query iot_device_detail{
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
     }`;
  return IOT_DEVICE_DETAIL;
};

const RETURN_IOT_CREDENTIALS = (iotproject: string, deviceId: string) => {
  const IOT_CREDENTIALS = gql`
    query iot_credentials{
      credentials(
        iotproject:"${iotproject}"
        deviceId: "${deviceId}"
      ) {
        total   
         credentials
        }
    }`;
  return IOT_CREDENTIALS;
};

const DELETE_IOT_DEVICE = gql`
  mutation deleteIotDevice($iotproject: String!, $deviceId: String!) {
    deleteIotDevice(iotproject: $iotproject, deviceId: $deviceId)
  }
`;

const RETURN_ALL_DEVICES_FOR_IOT_PROJECT_SORT = (sortBy?: ISortBy) => {
  let orderBy = "";
  if (sortBy) {
    switch (sortBy.index) {
      case 0:
        break;
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

const RETURN_ALL_DEVICES_FOR_IOT_PROJECT_FILTER = (
  filterObject: IDeviceFilter
) => {
  const { deviceId } = filterObject;
  let filter = "";

  if (deviceId) {
    filter += "`$.deviceId` = '" + deviceId + "'";
  }

  return filter;
};

const RETURN_ALL_DEVICES_FOR_IOT_PROJECT = (
  iotproject: string,
  sortBy?: ISortBy,
  filterObj?: IDeviceFilter
) => {
  let filter = RETURN_ALL_DEVICES_FOR_IOT_PROJECT_FILTER(
    filterObj || getInitialFilter()
  );

  let orderBy = RETURN_ALL_DEVICES_FOR_IOT_PROJECT_SORT(sortBy);

  const ALL_DEVICE_LIST = gql(
    `query devices_for_iot_project {
      devices(iotproject: "${iotproject}", orderBy:"${orderBy}", filter: "${filter}") {
        total
        devices {
          deviceId
          jsonData
          enabled
          viaGateway
        }
      }
    }`
  );

  return ALL_DEVICE_LIST;
};

const UPDATE_IOT_DEVICE = gql`
  mutation update_iot_device(
    $project: String!
    $device: Device_iot_console_input!
  ) {
    updateIotDevice(iotproject: $project, device: $device) {
      deviceId
    }
  }
`;

export {
  RETURN_IOT_DEVICE_DETAIL,
  RETURN_IOT_CREDENTIALS,
  DELETE_IOT_DEVICE,
  UPDATE_IOT_DEVICE,
  RETURN_ALL_DEVICES_FOR_IOT_PROJECT
};
