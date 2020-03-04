/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { SwitchWith404, LazyRoute } from "use-patternfly";
import { Redirect } from "react-router";

const getAddressSpaceListPage = () =>
  import("pages/AddressSpaceList/AddressSpaceListWithFilterAndPaginationPage");
const getAddressSpaceDetail = () =>
  import("pages/AddressSpaceDetail/AddressSpaceDetailPage");
const getAddressDetail = () => import("pages/AddressDetail/AddressDetailPage");
const getConnectionDetail = () =>
  import("pages/ConnectionDetail/ConnectionDetailPage");

const getConnectionsList = () =>
  import(
    "pages/AddressSpaceDetail/ConnectionList/ConnectionListWithFilterAndPaginationPage"
  );
const getAddressesList = () =>
  import(
    "pages/AddressSpaceDetail/AddressList/AddressesListWithFilterAndPaginationPage"
  );

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

export const AddressSpaceRoutes = () => (
  <SwitchWith404>
    <Redirect path="/" to="/address-spaces" exact={true} />
    <LazyRoute
      path="/address-spaces/:namespace/:name/:type/addresses/"
      getComponent={getAddressesList}
      exact={true}
    />
    <LazyRoute
      path="/address-spaces/:namespace/:name/:type/connections/"
      getComponent={getConnectionsList}
      exact={true}
    />
  </SwitchWith404>
);
