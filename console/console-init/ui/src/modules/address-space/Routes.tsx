/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { SwitchWith404, LazyRoute } from "use-patternfly";
import { Redirect } from "react-router";

const getConnections = () =>
  import(
    "pages/AddressSpaceDetail/ConnectionList/ConnectionListWithFilterAndPaginationPage"
  );
const getAddresses = () => import("modules/address/AddressPage");

export const Routes = () => (
  <SwitchWith404>
    <Redirect path="/" to="/address-spaces" exact={true} />
    <LazyRoute
      path="/address-spaces/:namespace/:name/:type/addresses/"
      getComponent={getAddresses}
      exact={true}
    />
    <LazyRoute
      path="/address-spaces/:namespace/:name/:type/connections/"
      getComponent={getConnections}
      exact={true}
    />
  </SwitchWith404>
);
