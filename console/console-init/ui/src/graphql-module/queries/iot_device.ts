/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import gql from "graphql-tag";

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

export { RETURN_IOT_DEVICE_DETAIL, RETURN_IOT_CREDENTIALS, DELETE_IOT_DEVICE };
