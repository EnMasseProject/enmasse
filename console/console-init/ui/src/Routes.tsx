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

export const AppRoutes = () => (
  // <SwitchWith404>
  //   {/* Redirect route */}
  //   <Redirect path="/" to="/projects" exact={true} />
  //   {/* messaging project list page route */}
  //   <LazyRoute
  //     path="/msg-projects"
  //     exact={true}
  //     getComponent={getAddressSpaceListPage}
  //   />
  //   {/* messaging project detial page route */}
  //   <LazyRoute
  //     path="/msg-projects/:namespace/:projectname/:type/:subList"
  //     exact={true}
  //     getComponent={getAddressSpaceDetail}
  //   />
  //   {/* address detail page route */}
  //   <LazyRoute
  //     path="/msg-projects/:namespace/:projectname/:type/addresses/:addressname"
  //     getComponent={getAddressDetail}
  //     exact={true}
  //   />
  //   {/* connection detail page route */}
  //   <LazyRoute
  //     path="/msg-projects/:namespace/:projectname/:type/connections/:connectionname"
  //     getComponent={getConnectionDetail}
  //     exact={true}
  //   />
  //   {/* iot-project detail page route */}
  //   <LazyRoute
  //     path="/iot-projects/:namespace/:projectname/:sublist"
  //     getComponent={getIoTProjectDetailPage}
  //     exact={true}
  //   />
  //   {/* iot-project device detail page route */}
  //   <LazyRoute
  //     path="/iot-projects/:namespace/:projectname/devices/:deviceid/:sublist"
  //     getComponent={getDeviceDEtailPage}
  //     exact={true}
  //   />
  // </SwitchWith404>
  <SwitchWith404>
    <Redirect path="/" to="/address-spaces" exact={true} />
    <LazyRoute
      path="/address-spaces"
      exact={true}
      getComponent={getAddressSpaceListPage}
    />
    <LazyRoute
      path="/projects"
      exact={true}
      getComponent={getProjectListPage}
    />
    <LazyRoute
      path="/address-spaces/:namespace/:name/:type/:subList"
      exact={true}
      getComponent={getAddressSpaceDetail}
    />
    <LazyRoute
      path="/address-spaces/:namespace/:name/:type/addresses/:addressname"
      getComponent={getAddressDetail}
      exact={true}
    />
    <LazyRoute
      path="/address-spaces/:namespace/:name/:type/connections/:connectionname"
      getComponent={getConnectionDetail}
      exact={true}
    />
<LazyRoute
      path="/iot-projects/:namespace/:projectname/devices/:deviceid/:subList"
      getComponent={getDeviceDetailPage}
      />
    <LazyRoute
      path="/iot-projects/:namespace/:projectname/:sublist"
      getComponent={getIoTProjectDetailPage}
      exact={true}
    />
  </SwitchWith404>
);
