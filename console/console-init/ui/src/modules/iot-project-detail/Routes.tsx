/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { SwitchWith404, LazyRoute } from "use-patternfly";

const getDevicesPage = () => import("modules/iot-device/DeviceListPage");
const IoTProjectDetailInfoPage = () =>
  import("modules/iot-project-detail/IoTProjectDetailInfoPage");
const getDeviceListPage = () => import("modules/iot-device/DeviceListPage");
const getCertificatesPage = () =>
  import("modules/iot-certificates/IoTCertificatesPage");

export const Routes = () => (
  <SwitchWith404>
    <LazyRoute
      path="/iot-projects/:namespace/:projectname/detail"
      getComponent={IoTProjectDetailInfoPage}
      exact={true}
    />
    <LazyRoute
      path="/iot-projects/:namespace/:projectname/devices"
      getComponent={getDeviceListPage}
      exact={true}
    />
    <LazyRoute
      path="/iot-projects/:namespace/:projectname/certificates"
      getComponent={getCertificatesPage}
      exact={true}
    />
    <LazyRoute
      path="/iot-projects/:namespace/:projectname/devices"
      getComponent={getDevicesPage}
      exact={true}
    />
  </SwitchWith404>
);
