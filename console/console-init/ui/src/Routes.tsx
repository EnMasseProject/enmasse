/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { SwitchWith404, LazyRoute } from "use-patternfly";
import { Redirect } from "react-router";

const getAddressSpaceListPage = () =>
  import("modules/address-space/AddressSpacePage");
const getProjectListPage = () => import("modules/project/ProjectPage");
const getAddressSpaceDetail = () =>
  import("modules/address-space/AddressSpaceDetailPage");
const getAddressDetail = () =>
  import("modules/address-detail/AddressDetailPage");
const getConnectionDetail = () =>
  import("modules/connection-detail/ConnectionDetailPage");
const getDeviceDetailPage = () =>
  import("modules/iot-device-detail/DeviceDetailPage");
const getIoTProjectDetailPage = () =>
  import("modules/iot-project-detail/IoTProjectDetailPage");
const getIoTCreateDevicePage = () =>
  import("modules/iot-device/dialogs/CreateDevice/CreateDevicePage");
const getIoTCreateDeviceUsingJsonPage = () =>
  import("modules/iot-device/dialogs/CreateDevice/CreateDeviceUsingJsonPage");

export const AppRoutes = () => (
  <SwitchWith404>
    <Redirect path="/" to="/projects" exact={true} />
    <LazyRoute
      path="/projects"
      exact={true}
      getComponent={getProjectListPage}
    />
    {/* messaging project list page route */}
    <LazyRoute
      path="/messaging-projects"
      exact={true}
      getComponent={getAddressSpaceListPage}
    />
    <LazyRoute
      path="/messaging-projects/:namespace/:name/:type/:subList"
      exact={true}
      getComponent={getAddressSpaceDetail}
    />
    <LazyRoute
      path="/messaging-projects/:namespace/:name/:type/addresses/:addressname"
      getComponent={getAddressDetail}
      exact={true}
    />
    <LazyRoute
      path="/messaging-projects/:namespace/:name/:type/connections/:connectionname"
      getComponent={getConnectionDetail}
      exact={true}
    />
    <LazyRoute
      path="/iot-projects/:namespace/:projectname/devices/:deviceid/:subList"
      getComponent={getDeviceDetailPage}
    />
    <LazyRoute
      path="/iot-projects/:namespace/:projectname/devices/addform"
      getComponent={getIoTCreateDevicePage}
      exact={true}
    />
    <LazyRoute
      path="/iot-projects/:namespace/:projectname/devices/addjson"
      getComponent={getIoTCreateDeviceUsingJsonPage}
      exact={true}
    />
    <LazyRoute
      path="/iot-projects/:namespace/:projectname/:sublist"
      getComponent={getIoTProjectDetailPage}
      exact={true}
    />
  </SwitchWith404>
);
