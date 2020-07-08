/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";
import { IDeviceFilter, getInitialFilter } from "modules/iot-device";
import { ISortBy } from "@patternfly/react-table";

const RETURN_IOT_DEVICE_DETAIL = (
  iotproject: string,
  deviceId: string,
  queryResolver?: string
) => {
  const defaultQueryResolver = `
        total
        devices{
          deviceId
          enabled
          viaGateway
          jsonData  
          credentials 
        }`;

  if (!queryResolver) {
    queryResolver = defaultQueryResolver;
  }

  const IOT_DEVICE_DETAIL = gql(
    `query iot_device_detail{
         devices(
             iotproject:"${iotproject}"
             filter: "\`$.deviceId\` = '${deviceId}'"){
              ${queryResolver}
          }
     }`
  );
  return IOT_DEVICE_DETAIL;
};

const RETURN_IOT_CREDENTIALS = (
  iotproject: string,
  deviceId: string,
  property?: string,
  filterValue?: string | boolean
) => {
  let filter = "";
  if (filterValue && property) {
    filter += "`$." + [property] + "` = '" + filterValue + "'";
  }
  const IOT_CREDENTIALS = gql(
    `query iot_credentials{
      credentials(
        filter:"${filter}",
        iotproject:"${iotproject}",
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
  `mutation delete_iot_device($iotproject: String!, $deviceId: [String!]!) {
    deleteIotDevices(iotproject: $iotproject, deviceIds: $deviceId)
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

const concatAND = (filter: string) => {
  if (filter?.trim() !== "") return " AND ";
  return "";
};

const FILTER_RETURN_ALL_DEVICES_FOR_IOT_PROJECT = (
  filterObject: IDeviceFilter
) => {
  const { deviceId, status, deviceType } = filterObject;
  let filter: string = "";

  if (deviceId && deviceId.trim() !== "") {
    filter += "`$.deviceId` = '" + deviceId + "'";
  }

  if (status?.trim() !== "" && status !== "allStatus") {
    filter += concatAND(filter);
    switch (status?.trim()) {
      case "disabled":
        filter += "`$.enabled`=false";
        break;
      case "enabled":
        filter += "`$.enabled`=true";
        break;
    }
  }

  if (deviceType?.trim() !== "" && deviceType?.trim() !== "allTypes") {
    filter += concatAND(filter);
    switch (deviceType?.trim()) {
      case "gateway":
        filter += "`$.viaGateway`=true";
        break;
      case "direct":
        filter += "`$.viaGateway`=false";
        break;
    }
  }

  // TODO: Needs to handle more parameters once the mock supports it.

  return filter;
};

const RETURN_ALL_DEVICES_FOR_IOT_PROJECT = (
  page: number,
  perPage: number,
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
      devices(iotproject: "${iotproject}",first:${perPage}, offset:${perPage *
      (page - 1)}, orderBy:"${orderBy}", filter: "${filter}") {
        ${queryResolver}
      }
    }`
  );

  return ALL_DEVICE_LIST;
};

const TOGGLE_IOT_DEVICE_STATUS = gql(
  `mutation toggle_iot_devices_status($a: ObjectMeta_v1_Input!, $b: [String!]!, $status: Boolean!){
    toggleIoTDevicesStatus(iotproject: $a, devices: $b, status: $status)
  }`
);

const SET_IOT_CREDENTIAL_FOR_DEVICE = gql(
  `mutation set_iot_credential_for_device(
    $iotproject: String!
    $deviceId: String!
    $jsonData: String!
  ){
    setCredentialsForDevice(
      iotproject:$iotproject,
      deviceId:$deviceId,
      jsonData:$jsonData
    )
  }
  `
);

export {
  RETURN_IOT_DEVICE_DETAIL,
  RETURN_IOT_CREDENTIALS,
  DELETE_IOT_DEVICE,
  RETURN_ALL_DEVICES_FOR_IOT_PROJECT,
  DELETE_CREDENTIALS_FOR_IOT_DEVICE,
  TOGGLE_IOT_DEVICE_STATUS,
  SET_IOT_CREDENTIAL_FOR_DEVICE
};
