/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { SwitchWith404, LazyRoute } from "use-patternfly";
import { Redirect } from "react-router";

const getDeviceInfoPage = () =>
  import("modules/iot-device-detail/DeviceInfoPage");
const getConfigurationInfoPage = () =>
  import("modules/iot-device-detail/ConfigurationInfoPage");

export const Routes = () => (
  <SwitchWith404>
    <Redirect path="/" to="/iot-project" exact={true} />
    <LazyRoute
      path="/iot-projects/:namespace/:projectname/:deviceid/deviceinfo"
      getComponent={getDeviceInfoPage}
      exact={true}
    />
    <LazyRoute
      path="/iot-projects/:namespace/:projectname/:deviceid/configurationinfo"
      getComponent={getConfigurationInfoPage}
      exact={true}
    />
  </SwitchWith404>
);
