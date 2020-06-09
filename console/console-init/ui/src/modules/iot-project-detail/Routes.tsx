/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { SwitchWith404, LazyRoute } from "use-patternfly";
import { Redirect } from "react-router";

const getCertificates = () => import("modules/address/AddressPage");

export const Routes = () => (
  <SwitchWith404>
    <Redirect path="/" to="/address-spaces" exact={true} />
    <LazyRoute
      path="/iot-projects/{namespace}/{projectname}/certificates"
      getComponent={getCertificates}
      exact={true}
    />
  </SwitchWith404>
);
