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
              }   
          }
     }`;
  return IOT_DEVICE_DETAIL;
};

export { RETURN_IOT_DEVICE_DETAIL };
