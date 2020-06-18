/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { SwitchWith404, LazyRoute } from "use-patternfly";

const getCertificates = () =>
  import("modules/iot-certificates/IoTCertificatesPage");
const getDevices = () => import("modules/iot-device/DeviceListPage");
const getProjectDetailPage = () =>
  import("modules/iot-project-detail/DetailPage");
const getDeviceListPage = () => import("modules/iot-device/DeviceListPage");
const getCertificatesPage = () =>
  import("modules/iot-certificates/IoTCertificatesPage");

export const Routes = () => (
  <SwitchWith404>
    <LazyRoute
      path="/iot-projects/:namespace/:projectname/detail"
      getComponent={getProjectDetailPage}
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
      getComponent={getDevices}
      exact={true}
    />
  </SwitchWith404>
);
