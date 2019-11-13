import * as React from "react";
import { SwitchWith404, LazyRoute } from "use-patternfly";
import { Redirect } from "react-router";

const getAddressSpaceListPage = () => import("./Pages/AddressSpaceListPage");
const getAddressSpaceDetail = () => import("./Pages/AddressSpaceDetailPage");
const getAddressDetail = () => import("./Pages/AddressDetailPage");
const getConnectionDetail = () => import("./Pages/ConnectionDetailPage");

export const AppRoutes = () => (
  <SwitchWith404>
    <Redirect path="/" to="/address-spaces" exact={true} />
    <LazyRoute
      path="/address-spaces"
      exact={true}
      getComponent={getAddressSpaceListPage}
    />
    <LazyRoute
      path="/address-space/:id/:subList"
      exact={true}
      getComponent={getAddressSpaceDetail}
    />
    <LazyRoute
      path="/address-space/:id/address/:id"
      getComponent={getAddressDetail}
    />
    <LazyRoute
      path="/address-space/:id/connection/:id"
      getComponent={getConnectionDetail}
    />
  </SwitchWith404>
);
