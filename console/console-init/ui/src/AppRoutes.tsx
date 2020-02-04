/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { SwitchWith404, LazyRoute } from "use-patternfly";
import { Redirect } from "react-router";

const getAddressSpaceListPage = () =>
  import(
    "./Pages/AddressSpaceList/AddressSpaceListWithFilterAndPaginationPage"
  );
const getAddressSpaceDetail = () =>
  import("./Pages/AddressSpaceDetail/AddressSpaceDetailPage");
const getAddressDetail = () =>
  import("./Pages/AddressDetail/AddressDetailPage");
const getConnectionDetail = () =>
  import("./Pages/ConnectionDetail/ConnectionDetailPage");

export const AppRoutes = () => (
  <SwitchWith404>
    <Redirect path="/" to="/address-spaces" exact={true} />
    <LazyRoute
      path="/address-spaces"
      exact={true}
      getComponent={getAddressSpaceListPage}
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
  </SwitchWith404>
);
