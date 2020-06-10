/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { SwitchWith404, LazyRoute } from "use-patternfly";
import { Redirect } from "react-router";

const getConnections = () => import("modules/connection/ConnectionPage");
const getAddresses = () => import("modules/address/AddressPage");

export const Routes = () => (
  <SwitchWith404>
    <Redirect path="/" to="/messaging-projects" exact={true} />
    <LazyRoute
      path="/messaging-projects/:namespace/:name/:type/addresses/"
      getComponent={getAddresses}
      exact={true}
    />
    <LazyRoute
      path="/messaging-projects/:namespace/:name/:type/connections/"
      getComponent={getConnections}
      exact={true}
    />
  </SwitchWith404>
);
